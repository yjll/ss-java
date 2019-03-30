package com.yjll.ss.nio;

import com.yjll.ss.encryption.CryptFactory;
import com.yjll.ss.encryption.ICrypt;
import com.yjll.ss.utils.ByteBuffers;
import com.yjll.ss.encryption.CryptHelper;
import com.yjll.ss.utils.SSConfig;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author: zijing
 * @date: 2019/2/26 19:21
 * @description:
 */
@Slf4j
public class ChannelHandler {

    private SSConfig ssConfig;

    private Selector selector;

    private SocketChannel localSocketChannel;

    private SocketChannel remoteSocketChannel;

    private CryptHelper cryptHelper;
    /**
     * 传输数据队列
     */
    private List<byte[]> byteQueue = new ArrayList<>();

    private ByteBuffer byteBuffer = ByteBuffer.allocate(64 * 1024);

    private SocksStage stage = SocksStage.STAGE_INIT;

    public ChannelHandler(SSConfig ssConfig, Selector selector, SocketChannel socketChannel) {
        this.ssConfig = ssConfig;
        this.selector = selector;
        this.localSocketChannel = socketChannel;
        ICrypt crypt = CryptFactory.get(ssConfig.getMethod(), ssConfig.getPassword());
        cryptHelper = new CryptHelper(crypt);
    }

    public void handle(SelectionKey key) {
        SocketChannel channel = (SocketChannel) key.channel();
        try {
            if (localSocketChannel == channel) {
                if (key.isReadable()) {
                    onLocalRead(channel);
                }
            } else if (remoteSocketChannel == channel) {
                if (key.isConnectable()) {
                    onRemoteConnect(key, channel);
                }
                if (key.isReadable()) {
                    onRemoteRead(channel);
                }
            }
        } catch (Exception e) {
            log.error(e.toString(), e);
            this.stage = SocksStage.STAGE_DESTROYED;
        }

        if (this.stage == SocksStage.STAGE_DESTROYED) {
            destroy();
        }
    }

    /**
     * 读取local端发送数据
     *
     * @param socketChannel
     */
    private void onLocalRead(SocketChannel socketChannel) throws IOException {
        SocksStage socksStage = this.stage;

        int read = socketChannel.read(byteBuffer);
        byte[] readData = ByteBuffers.convertByte(byteBuffer);
        if (readData.length == 0) {
            this.stage = SocksStage.STAGE_DESTROYED;
            return;
        }

        log.info("status:{},readData:{}", socksStage, new String(readData));

        switch (socksStage) {
            /*
                +----+----------+----------+
                |VER | NMETHODS | METHODS  |
                +----+----------+----------+
                | 1  |    1     |  1~255   |
                +----+----------+----------+
             */
            case STAGE_INIT:

                socketChannel.write(ByteBuffer.wrap(new byte[]{0x05, 0x00}));
                this.stage = SocksStage.STAGE_ADDR;
                break;

            /*
                +----+-----+-------+------+----------+----------+
                |VER | CMD |  RSV  | ATYP | DST.ADDR | DST.PORT |
                +----+-----+-------+------+----------+----------+
                | 1  |  1  |   1   |  1   | Variable |    2     |
                +----+-----+-------+------+----------+----------+
             */
            case STAGE_ADDR:

                remoteSocketChannel = SocketChannel.open();
                remoteSocketChannel.configureBlocking(false);
                remoteSocketChannel.register(this.selector, SelectionKey.OP_CONNECT,this);
                remoteSocketChannel.connect(new InetSocketAddress(ssConfig.getServer(), ssConfig.getServerPort()));

                socketChannel.write(ByteBuffer.wrap(new byte[]{0x05, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x10, 0x10}));
                this.stage = SocksStage.STAGE_CONNECTING;
                // 去掉无用数据,缓存向服务端请求的数据
                this.byteQueue.add(Arrays.copyOfRange(readData, 3, read));
                break;
            case STAGE_CONNECTING:
                this.byteQueue.add(readData);
                break;
            case STAGE_STREAM:
                byte[] encrypt = this.cryptHelper.encrypt(readData);
//                socketChannel.write(ByteBuffer.wrap(encrypt));
                this.remoteSocketChannel.write(ByteBuffer.wrap(encrypt));
                break;
        }
    }

    private void onRemoteConnect(SelectionKey key, SocketChannel socketChannel) throws IOException {
        if (socketChannel.finishConnect()) {
            key.interestOps(SelectionKey.OP_READ);
            this.stage = SocksStage.STAGE_STREAM;
            for (byte[] bytes : byteQueue) {
                byte[] encrypt = this.cryptHelper.encrypt(bytes);
                socketChannel.write(ByteBuffer.wrap(encrypt));
            }
        }
    }

    private void onRemoteRead(SocketChannel socketChannel) throws IOException {
        socketChannel.read(byteBuffer);
        byte[] readData = ByteBuffers.convertByte(byteBuffer);
        if (readData.length == 0) {
            this.stage = SocksStage.STAGE_DESTROYED;
            return;
        }
        byte[] decrypt = this.cryptHelper.decrypt(readData);
        log.info("onRemoteRead:{}", new String(readData));
        this.localSocketChannel.write(ByteBuffer.wrap(decrypt));
    }

    private void destroy() {
        if (this.localSocketChannel != null) {
            try {
                this.localSocketChannel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (this.remoteSocketChannel != null) {
            try {
                this.remoteSocketChannel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    enum SocksStage {
        STAGE_INIT(0),
        STAGE_ADDR(1),
        STAGE_CONNECTING(4),
        STAGE_STREAM(5),
        STAGE_DESTROYED(-1);
        private Integer stage;

        public Integer getStage() {
            return stage;
        }

        SocksStage(Integer stage) {
            this.stage = stage;
        }

        public static SocksStage getByStage(int stage) {
            for (SocksStage socksStage : values()) {
                if (socksStage.getStage() == stage) {
                    return socksStage;
                }
            }
            return STAGE_INIT;
        }
    }


}

package com.yjll.ss.nio;

import com.yjll.ss.encryption.CryptFactory;
import com.yjll.ss.encryption.CryptHelper;
import com.yjll.ss.encryption.ICrypt;
import com.yjll.ss.utils.ByteBuffers;
import com.yjll.ss.utils.SSConfig;
import com.yjll.ss.utils.Socks5Utils;
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

    /**
     * 与本地之间的连接
     */
    private SocketChannel localSocketChannel;
    /**
     * 与远程服务之间的连接
     */
    private SocketChannel remoteSocketChannel;
    /**
     * 加密工具
     */
    private CryptHelper cryptHelper;
    /**
     * 传输数据队列
     */
    private List<byte[]> byteQueue = new ArrayList<>();
    /**
     * 缓冲
     */
    private ByteBuffer byteBuffer = ByteBuffer.allocate(64 * 1024);
    /**
     * 状态默认初始状态
     */
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
            // 与浏览器交互的Channel
            if (localSocketChannel == channel) {
                // 当有request时，读取数据
                if (key.isReadable()) {
                    onLocalRead(channel);
                }
                // 与远程服务交互的Channel
            } else if (remoteSocketChannel == channel) {
                // 远程可连接时，发送加密数据包
                if (key.isConnectable()) {
                    onRemoteConnect(key, channel);
                }
                // 当远程返回数据时，进行解密，将解密数据返回给浏览器
                if (key.isReadable()) {
                    onRemoteRead(channel);
                }
            }
        } catch (Exception e) {
            log.error(e.toString(), e);
            this.stage = SocksStage.STAGE_DESTROYED;
        }

        // 销毁
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

//        log.debug("status:{}", socksStage);

        // 通过状态控制socks5交互的步骤
        switch (socksStage) {
            /*
                +----+----------+----------+
                |VER | NMETHODS | METHODS  |
                +----+----------+----------+
                | 1  |    1     |  1~255   |
                +----+----------+----------+
             */
            // 握手，告知支持的协议
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

            // request中有要访问的域名，此处需要与远程建立连接，订阅OP_CONNECT事件，当可连接时通知到我们。
            case STAGE_ADDR:

                remoteSocketChannel = SocketChannel.open();
                remoteSocketChannel.configureBlocking(false);
                remoteSocketChannel.register(this.selector, SelectionKey.OP_CONNECT, this);
                remoteSocketChannel.connect(new InetSocketAddress(ssConfig.getServer(), ssConfig.getServerPort()));

                socketChannel.write(ByteBuffer.wrap(new byte[]{0x05, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x10, 0x10}));
                this.stage = SocksStage.STAGE_CONNECTING;
                // 0x05 0x01 0x00 0x03 0x0a b'google.com'  0x00 0x50
                // 0x0a 表示域名长度
                Socks5Utils.parseAddr(readData);
                // 去掉无用数据,缓存向服务端请求的数据
                this.byteQueue.add(Arrays.copyOfRange(readData, 3, read));
                break;
            case STAGE_CONNECTING:
                // 远程未连接，但是客户端数据已经到达，将数据缓存到队列中
                this.byteQueue.add(readData);
                break;
            case STAGE_STREAM:
                byte[] encrypt = this.cryptHelper.encrypt(readData);
//                socketChannel.write(ByteBuffer.wrap(encrypt));
                // 将数据加密发送给远程服务器
                this.remoteSocketChannel.write(ByteBuffer.wrap(encrypt));
                break;
        }
    }

    /**
     * 远程服务可连接
     * @param key
     * @param socketChannel
     * @throws IOException
     */
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

    /**
     * 远程服务可读
     * @param socketChannel
     * @throws IOException
     */
    private void onRemoteRead(SocketChannel socketChannel) throws IOException {
        socketChannel.read(byteBuffer);
        byte[] readData = ByteBuffers.convertByte(byteBuffer);
        if (readData.length == 0) {
            this.stage = SocksStage.STAGE_DESTROYED;
            return;
        }
        byte[] decrypt = this.cryptHelper.decrypt(readData);
//        log.debug("onRemoteRead:\n{}", new String(decrypt));
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
        /**
         * 初始状态
         */
        STAGE_INIT(0),
        /**
         * 远程地址
         */
        STAGE_ADDR(1),
        /**
         * 连接中
         */
        STAGE_CONNECTING(4),
        /**
         * 向远程发送数据
         */
        STAGE_STREAM(5),
        /**
         * 销毁
         */
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

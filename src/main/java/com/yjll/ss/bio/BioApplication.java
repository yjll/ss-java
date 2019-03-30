package com.yjll.ss.bio;


import com.google.common.primitives.Bytes;
import com.yjll.ss.encryption.CryptFactory;
import com.yjll.ss.utils.ConfigFactory;
import com.yjll.ss.encryption.CryptHelper;
import com.yjll.ss.utils.SSConfig;
import com.yjll.ss.utils.Socks5Utils;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author: zijing
 * @date: 2019/1/14 11:17
 * @description:
 */
@Slf4j
public class BioApplication {


    private SSConfig ssConfig;

    public BioApplication(SSConfig ssConfig) {
        this.ssConfig = ssConfig;
    }

    private ThreadLocal<CryptHelper> cryptHelperThreadLocal = ThreadLocal.withInitial(
            () -> new CryptHelper(CryptFactory.get(ssConfig.getMethod(), ssConfig.getPassword())));


    public void execute() throws IOException {
        log.info("starting server at port {}", ssConfig.getLocalPort());

        ServerSocket serverSocket = new ServerSocket(ssConfig.getLocalPort());
        while (true) {
            Socket socket = serverSocket.accept();
            new Thread(() -> {
                try {
                    handle(socket);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    cryptHelperThreadLocal.remove();
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }

    private void handle(Socket socket) throws Exception {
        CryptHelper cryptHelper = cryptHelperThreadLocal.get();
        byte[] readData;
        InputStream localInputStream = socket.getInputStream();
        OutputStream localOutputStream = socket.getOutputStream();

        // step1  握手
        readData = getBytesFromInputStream(localInputStream);
        localOutputStream.write(new byte[]{0x05, 0x00});

        // step2 连接目标服务器
        readData = getBytesFromInputStream(localInputStream);

        Socks5Utils.parseAddr(readData);
        localOutputStream.write(new byte[]{0x05, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x10, 0x10});
        // 连接远程服务器
        Socket remote = new Socket(ssConfig.getServer(), ssConfig.getServerPort());
        OutputStream remoteOutputStream = remote.getOutputStream();
        InputStream remoteInputStream = remote.getInputStream();

        byte[] bytesData = Arrays.copyOfRange(readData, 3, readData.length);
        remoteOutputStream.write(cryptHelper.encrypt(bytesData));

        // step3 发送Http报文
        readData = getBytesFromInputStream(localInputStream);
        log.info("HTTP报文:\n{}", new String(readData));
        remoteOutputStream.write(cryptHelper.encrypt(readData));


        byte[] bytes = getBytesFromInputStream(remoteInputStream);
        byte[] decrypt = cryptHelper.decrypt(bytes);
        log.info("Response:\n{}", new String(decrypt));
        localOutputStream.write(decrypt);
        localOutputStream.flush();
    }

    private byte[] getBytesFromInputStream(InputStream is) throws IOException {
        List<Byte> list = new ArrayList<>();
        byte[] tmp = new byte[8 * 1024];
        int read2;
        do {
            read2 = is.read(tmp);
            if (read2 > 0) {
                list.addAll(Bytes.asList(Arrays.copyOf(tmp, read2)));
            }
        } while (read2 == tmp.length);
        return Bytes.toArray(list);
    }
}

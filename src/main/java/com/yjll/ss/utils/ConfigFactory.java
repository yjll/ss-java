package com.yjll.ss.utils;

import com.yjll.ss.encryption.impl.AesCrypt;

/**
 * @author: zijing
 * @date: 2019/3/27 14:04
 * @description:
 */
public class ConfigFactory {

    private static final SSConfig ssConfig = SSConfig.builder()
            .localPort(9997)
            .server("127.0.0.1")
            .serverPort(8388)
            .password("admin")
            .method(AesCrypt.CIPHER_AES_256_CFB)
            .timeOut(300)
            .build();

    public static SSConfig getDefaultSSConfig(){
        return ssConfig;
    }
}

package com.yjll.ss.utils;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.yjll.ss.encryption.impl.AesCrypt;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * @author: zijing
 * @date: 2019/3/27 14:04
 * @description:
 */
public class ConfigFactory {

    public static SSConfig getDefaultSSConfig() {
        return SSConfig.builder()
                .localPort(9997)
                .server("127.0.0.1")
                .serverPort(8388)
                .password("admin")
                .method(AesCrypt.CIPHER_AES_256_CFB)
                .timeOut(300)
                .build();
    }

    public static SSConfig getSsConfigFromResources() {
        String configFile = "config.json";
        InputStream resourceAsStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(configFile);
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
        Gson gson = gsonBuilder.create();
        return gson.fromJson(new InputStreamReader(resourceAsStream), SSConfig.class);
    }

}

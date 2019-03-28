package com.yjll.ss.encryption;

import com.yjll.ss.utils.SSConfig;

import java.io.ByteArrayOutputStream;

/**
 * @author: zijing
 * @date: 2019/3/26 19:04
 * @description:
 */
public class CryptHelper {

    private SSConfig ssConfig;

    private ICrypt iCrypt;

    public CryptHelper(ICrypt crypt) {
        this.iCrypt= crypt;
    }

    public byte[] encrypt(byte[] bytes) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        iCrypt.encrypt(bytes, bytes.length,byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
//        return bytes;
    }

    public byte[] decrypt(byte[] bytes) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        iCrypt.decrypt(bytes, bytes.length, byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }

}

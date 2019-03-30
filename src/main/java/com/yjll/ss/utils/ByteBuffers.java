package com.yjll.ss.utils;

import java.nio.ByteBuffer;

/**
 * @author: zijing
 * @date: 2019/2/27 16:40
 * @description:
 */
public class ByteBuffers {

    private ByteBuffers() {
    }

    static public byte[] convertByte(ByteBuffer byteBuffer) {
        byteBuffer.flip();
        byte[] tmp = new byte[byteBuffer.remaining()];
        byteBuffer.get(tmp);
        byteBuffer.clear();
        return tmp;
    }
}

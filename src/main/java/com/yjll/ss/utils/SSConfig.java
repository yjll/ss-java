package com.yjll.ss.utils;

import com.yjll.ss.encryption.impl.AesCrypt;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author: zijing
 * @date: 2019/2/26 14:11
 * @description:
 */

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SSConfig {
    private String server;

    private Integer serverPort;

    private String localAddress;

    private Integer localPort;

    private String password;

    private Integer timeOut;

    private String method;



}

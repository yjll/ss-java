package com.yjll.ss.utils;

import lombok.extern.slf4j.Slf4j;

import java.math.BigInteger;
import java.util.Arrays;

@Slf4j
public class Socks5Utils {

	public static void parseAddr(byte[] readData){
		// ATYP值为3表示通过域名访问
		if(readData[3] == 0x03) {
			// 域名长度下标
			int DOMAIN_LENGTH_INDEX = 4;
			// 域名下标
			int DOMAIN_INDEX = 5;
			// 域名长度
			byte domainLength = readData[DOMAIN_LENGTH_INDEX];
			String domain = new String(Arrays.copyOfRange(readData, DOMAIN_INDEX, DOMAIN_INDEX + domainLength));
			int port = new BigInteger(new byte[]{readData[DOMAIN_INDEX + domainLength], readData[DOMAIN_INDEX + 1 + domainLength]}).intValue();
			log.debug("target url and port {}:{}", domain, port);
		}
	}


	
}

package com.yjll.ss;

import com.yjll.ss.client.bio.BioApplication;
import com.yjll.ss.utils.ConfigFactory;
import com.yjll.ss.utils.SSConfig;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * @author: zijing
 * @date: 2019/3/30 12:45
 * @description:
 */
@Slf4j
public class Application {
    public static void main(String[] args) {
        SSConfig ssConfig = ConfigFactory.getSsConfigFromResources();
        BioApplication app = new BioApplication(ssConfig);
//        NioApplication app = new NioApplication(ssConfig);
        try {
            app.execute();
        } catch (IOException e) {
            log.error(e.toString(), e);
        }

    }
}

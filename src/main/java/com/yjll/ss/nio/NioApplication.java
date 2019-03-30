package com.yjll.ss.nio;

import com.yjll.ss.utils.ConfigFactory;
import com.yjll.ss.utils.SSConfig;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;

/**
 * @author: zijing
 * @date: 2019/2/26 14:08
 * @description:
 */
@Slf4j
public class NioApplication {

    private static final SSConfig ssConfig = ConfigFactory.getDefaultSSConfig();

    public static void main(String[] args) throws IOException, InterruptedException {
        log.info("starting server at port {}", ssConfig.getLocalPort());

        Selector selector = Selector.open();
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.socket().bind(new InetSocketAddress(ssConfig.getLocalPort()));
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        while (true) {
            int count = selector.select();
            if (count <= 0) continue;
            log.info("selector_count:{}", count);

            Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
            while (iterator.hasNext()) {

                SelectionKey selectionKey = iterator.next();
                iterator.remove();

                if (!selectionKey.isValid()) continue;

//                // 第一次
                if (selectionKey.isAcceptable()) {
                    ServerSocketChannel channel = (ServerSocketChannel) selectionKey.channel();
                    SocketChannel socketChannel = channel.accept();
                    socketChannel.configureBlocking(false);
                    socketChannel.register(selector, SelectionKey.OP_READ, new ChannelHandler(ssConfig, selector, socketChannel));
                } else {
                    ChannelHandler channelHandler = (ChannelHandler) selectionKey.attachment();
                    if (Objects.nonNull(channelHandler)) {
                        channelHandler.handle(selectionKey);
                    }
                }


            }
        }


    }
}
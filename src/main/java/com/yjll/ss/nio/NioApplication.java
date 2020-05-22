package com.yjll.ss.nio;

import com.yjll.ss.utils.SSConfig;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Objects;

/**
 * @author: zijing
 * @date: 2019/2/26 14:08
 * @description:
 */
@Slf4j
public class NioApplication {

    private SSConfig ssConfig;

    public NioApplication(SSConfig ssConfig) {
        this.ssConfig = ssConfig;
    }

    public void execute() throws IOException {

        log.info("starting server at port {}", ssConfig.getLocalPort());

        Selector selector = Selector.open();
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.socket().bind(new InetSocketAddress(ssConfig.getLocalPort()));
        // 非阻塞
        serverSocketChannel.configureBlocking(false);
        // 订阅事件，当有请求时激活
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        while (true) {
            // 阻塞线程，直到有订阅的事件
            int count = selector.select();
            if (count <= 0) continue;

            Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
            while (iterator.hasNext()) {

                SelectionKey selectionKey = iterator.next();
                iterator.remove();

                if (!selectionKey.isValid()) continue;

                // 一个新的连接进来时，
                if (selectionKey.isAcceptable()) {
                    ServerSocketChannel channel = (ServerSocketChannel) selectionKey.channel();
                    // 新进来的SocketChannel
                    SocketChannel socketChannel = channel.accept();
                    socketChannel.configureBlocking(false);
                    // 我们希望从中获取数据，我们注册读事件，当数据可读时会通知我们
                    // 并绑定一个处理器，当数据可读时，使用这个处理器进行数据处理
                    socketChannel.register(selector, SelectionKey.OP_READ, new ChannelHandler(ssConfig, selector, socketChannel));
                } else {
                    // 交由对应的处理器来处理
                    ChannelHandler channelHandler = (ChannelHandler) selectionKey.attachment();
                    if (Objects.nonNull(channelHandler)) {
                        channelHandler.handle(selectionKey);
                    }
                }
            }
        }


    }
}

package com.yjll.ss.server.netty;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpServerCodec;

/**
 * @author: zijing
 * @date: 2019/4/3 17:56
 * @description:
 */
public class TestInitializer extends ChannelInitializer<SocketChannel> {
    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();

        pipeline.addLast("http",new HttpServerCodec());
        pipeline.addLast("test",new TestHttpHandler());
    }
}

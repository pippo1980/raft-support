package com.baidu.raft.endpoint.tcp;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by pippo on 16/7/27.
 */
public class CustomEndpointInitializer extends ChannelInitializer<SocketChannel> {

    private static Logger logger = LoggerFactory.getLogger(CustomEndpointInitializer.class);

    public CustomEndpointInitializer(ChannelHandler messageHandler) {
        this.messageHandler = messageHandler;
    }

    protected ChannelHandler messageHandler;

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();

        // inbound
        pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(1024 * 1024, 0, 4, 0, 4));
        pipeline.addLast("raftMessageDecoder", new RaftMessageDecoder());

        // outbound
        pipeline.addLast("frameEncoder", new LengthFieldPrepender(4));
        pipeline.addLast("raftMessageEncoder", new RaftMessageEncoder());

        // session  handler
        pipeline.addLast("raftSessionHandler", new RaftSessionHandler());

        // message handler
        pipeline.addLast("messageHandler", messageHandler);

        logger.trace("init channel:[remote={},local:{}] with handlers:[{}]",
                ch.remoteAddress(),
                ch.localAddress(),
                ch.pipeline().toMap());
    }

}

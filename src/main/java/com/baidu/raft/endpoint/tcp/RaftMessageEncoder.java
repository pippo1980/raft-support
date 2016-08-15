package com.baidu.raft.endpoint.tcp;

import com.baidu.raft.protocol.BaseMessage;
import com.baidu.raft.utils.JSONUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * Created by pippo on 16/7/27.
 */
public class RaftMessageEncoder extends MessageToByteEncoder<BaseMessage> {

    @Override
    protected void encode(ChannelHandlerContext ctx, BaseMessage msg, ByteBuf out) throws Exception {
        try {
            out.writeByte(msg.type.code);
            out.writeBytes(JSONUtil.toBytes(msg));
        } catch (Throwable cause) {
            ctx.fireExceptionCaught(cause);
        }

    }
}
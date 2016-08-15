package com.baidu.raft.endpoint.tcp;

import com.baidu.raft.protocol.Heartbeat;
import com.baidu.raft.protocol.JoinClusterRQ;
import com.baidu.raft.protocol.JoinClusterRS;
import com.baidu.raft.protocol.MessageType;
import com.baidu.raft.protocol.VoteRQ;
import com.baidu.raft.protocol.VoteRS;
import com.baidu.raft.utils.JSONUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

import java.util.List;

/**
 * Created by pippo on 16/7/27.
 */
public class RaftMessageDecoder extends MessageToMessageDecoder<ByteBuf> {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
        try {
            byte code = msg.readByte();

            Class<?> clazz = null;
            switch (MessageType.from(code)) {
                case Heartbeat:
                    clazz = Heartbeat.class;
                    break;
                case VoteRQ:
                    clazz = VoteRQ.class;
                    break;
                case VoteRS:
                    clazz = VoteRS.class;
                    break;
                case JoinClusterRQ:
                    clazz = JoinClusterRQ.class;
                    break;
                case JoinClusterRS:
                    clazz = JoinClusterRS.class;
                    break;
            }

            try {
                byte[] payload = new byte[msg.readableBytes()];
                msg.readBytes(payload);
                Object message = JSONUtil.toObject(payload, clazz);
                out.add(message);
            } catch (Exception e) {
                ctx.fireExceptionCaught(e);
            }

        } catch (Exception e) {
            ctx.fireExceptionCaught(e);
        }
    }

    private byte[] read(ByteBuf msg) {
        byte[] array;
        if (msg.hasArray()) {
            if (msg.arrayOffset() == 0 && msg.readableBytes() == msg.capacity()) {
                // we have no offset and the length is the same as the capacity. Its safe to reuse
                // the array without copy it first
                array = msg.array();
            } else {
                // copy the ChannelBuffer to a byte array
                array = new byte[msg.readableBytes()];
                msg.getBytes(0, array);
            }
        } else {
            // copy the ChannelBuffer to a byte array
            array = new byte[msg.readableBytes()];
            msg.getBytes(0, array);
        }
        return array;
    }

    public static void main(String[] args) {
        String json = "{\"type\":1,\"source\":1,\"destination\":1,\"term\":3,\"lastIndex\":0,\"lastTerm\":1,\"leader\":1}";
        JSONUtil.toObject(json.getBytes(), VoteRQ.class);
    }
}

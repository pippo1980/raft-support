package com.baidu.raft.endpoint.tcp;

import com.baidu.raft.PeerRole;
import com.baidu.raft.election.ElectionStat;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by pippo on 16/8/5.
 */
public class RaftSessionHandler extends ChannelDuplexHandler {

    private static Logger logger = LoggerFactory.getLogger(RaftMessageHandler.class);

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Integer nodeId = (Integer) ctx.channel().attr(AttributeKey.valueOf(ctx.channel().remoteAddress().toString())).get();
        if (nodeId == null || nodeId <= 0) {
            return;
        }

        if (nodeId == ElectionStat.get().getLeader()) {
            ElectionStat.get().process(stat -> {
                logger.info("leader:[{}] is offline, change current role to candidate", nodeId);
                stat.changeRole(stat.getCurrentTerm(), PeerRole.Candidate);
                return stat;
            });
        }
    }

}

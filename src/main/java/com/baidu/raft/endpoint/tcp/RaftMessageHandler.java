package com.baidu.raft.endpoint.tcp;

import com.baidu.raft.PeerRole;
import com.baidu.raft.RaftCluster;
import com.baidu.raft.election.ElectionStat;
import com.baidu.raft.protocol.BaseMessage;
import com.baidu.raft.protocol.Heartbeat;
import com.baidu.raft.protocol.JoinClusterRQ;
import com.baidu.raft.protocol.JoinClusterRS;
import com.baidu.raft.protocol.VoteRQ;
import com.baidu.raft.protocol.VoteRS;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

/**
 * Created by pippo on 16/7/27.
 */
@ChannelHandler.Sharable
public class RaftMessageHandler extends ChannelDuplexHandler {

    private static Logger logger = LoggerFactory.getLogger(RaftMessageHandler.class);

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        if (!(msg instanceof BaseMessage)) {
            logger.warn("dropped unknown msg:[{}]", msg);
            return;
        }

        BaseMessage message = (BaseMessage) msg;

        if (!validate(ctx, message)) {
            return;
        }

        switch (message.type) {
            case Heartbeat:
                // logger.debug("received heartbeat:[{}]", message);
                ElectionStat.get().process((Heartbeat) message);
                break;
            case VoteRQ:
                logger.debug("received vote rq:[{}]", message);
                ElectionStat.get().process((VoteRQ) message);
                break;
            case VoteRS:
                logger.debug("received vote rs:[{}]", message);
                ElectionStat.get().process((VoteRS) message);
                break;
            case JoinClusterRQ:
                logger.debug("received node join rq:[{}]", message);
                RaftCluster.get().join((JoinClusterRQ) message);
                break;
            case JoinClusterRS:
                logger.debug("received node join rs:[{}]", message);
                RaftCluster.get().update((JoinClusterRS) message);
                break;
            default:
                logger.warn("can not process message:[{}]", message);
        }

    }

    private boolean validate(ChannelHandlerContext ctx, BaseMessage message) {
        // 在channel上记录endpointId与channel的关系,方便端口连接的时候
        InetSocketAddress address = (InetSocketAddress) ctx.channel().remoteAddress();
        ctx.channel().attr(AttributeKey.valueOf(address.toString())).setIfAbsent(message.source);

        long currentTerm = ElectionStat.get().getCurrentTerm();

        // 新节点加入的时候,term是0
        // 如果当前集群已经运行了很久,那么term会很大,新节点消息会被丢弃
        // 通过JoinClusterRQ和JoinClusterRS还让新节点快速更新到最新的term
        if (message instanceof JoinClusterRQ) {
            message.setTerm(currentTerm);
        }

        // 忽略过期消息
        if (message.term < currentTerm) {
            logger.info("dropped expired term:[current={}] message:[{}]", currentTerm, message);
            return false;
        }

        // 如果当前节点已经过期,那么需要改变当前节点状态,重新获取leader
        if (message.term > currentTerm) {
            logger.info("message:[{}] large then current term:[{}], change current node role to candidate",
                    message,
                    currentTerm);
            ElectionStat.get().changeRole(message.term, PeerRole.Candidate);
            return true;
        }

        return true;
    }

}

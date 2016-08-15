package com.baidu.raft.election;

import com.baidu.raft.PeerRole;
import com.baidu.raft.RaftCluster;
import com.baidu.raft.protocol.VoteRS;
import com.baidu.raft.storage.LogEntry;
import com.baidu.raft.storage.LogType;
import com.baidu.raft.utils.JSONUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by pippo on 16/8/2.
 */
public class VoteRSProcessor implements ElectionStatProcessor<Boolean> {

    private static Logger logger = LoggerFactory.getLogger(VoteRSProcessor.class);

    public VoteRSProcessor(VoteRS rs) {
        this.rs = rs;
    }

    private VoteRS rs;

    @Override
    public Boolean process(ElectionStat stat) {
        boolean accept = accept(stat);

        if (accept) {
            stat.changeRole(rs.term, PeerRole.Leader);
            RaftCluster.get().getLogStorage().append(new LogEntry(LogType.Application,
                    rs.term,
                    JSONUtil.toBytes(rs)));
            logger.info("current node become leader of term:[{}]", stat);
        }

        return accept;
    }

    private boolean accept(ElectionStat stat) {
        if (!rs.accept) {
            return false;
        }

        int count = stat.acceptCount.get();

        // 说明当前节点发起的选举已经过期,将状态重置,等待后续选举
        if (rs.term > stat.currentTerm.get()) {
            stat.currentTerm.set(rs.term);
        } else {
            count = stat.acceptCount.incrementAndGet();
        }

        int acceptThreshold = (RaftCluster.get().getAcceptThreshold() / 2 + 1);
        return count >= acceptThreshold;
    }
}

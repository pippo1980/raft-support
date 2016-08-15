package com.baidu.raft.election;

import com.baidu.raft.PeerRole;
import com.baidu.raft.RaftCluster;
import com.baidu.raft.protocol.Heartbeat;
import com.baidu.raft.storage.LogEntry;
import com.baidu.raft.storage.LogType;
import com.baidu.raft.utils.JSONUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by pippo on 16/7/27.
 */
public class HeartbeatProcessor implements ElectionStatProcessor<Boolean> {

    private static Logger logger = LoggerFactory.getLogger(HeartbeatProcessor.class);

    public HeartbeatProcessor(Heartbeat heartbeat) {
        this.heartbeat = heartbeat;
    }

    private Heartbeat heartbeat;

    @Override
    public Boolean process(ElectionStat stat) {
        // TODO 如果heartbeat的leader与当前leader不符合(应该不会有这样的情况)

        // 当前节点已经是follower
        if (stat.currentRole == PeerRole.Follower && stat.leader > 0) {
            return true;
        }

        // 接受leader,成为follower
        boolean accept = accept(stat);

        if (accept) {
            logger.info("accept node:[{}] as leader of term:[{}]", heartbeat.leader, heartbeat.term);
            // raftCluster.changeRole(PeerRole.Follower);
            RaftCluster.get().getLogStorage().append(new LogEntry(LogType.LogPack,
                    heartbeat.term,
                    JSONUtil.toBytes(heartbeat)));
        }

        return accept;
    }

    private boolean accept(ElectionStat stat) {
        if (heartbeat.term < stat.currentTerm.get()) {
            return false;
        }

        return stat.changeRole(heartbeat.term, PeerRole.Follower) && stat.setLeader(heartbeat.term, heartbeat.leader);
    }
}

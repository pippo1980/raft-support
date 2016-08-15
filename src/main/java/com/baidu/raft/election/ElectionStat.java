package com.baidu.raft.election;

import com.baidu.raft.PeerMain;
import com.baidu.raft.PeerRole;
import com.baidu.raft.RaftCluster;
import com.baidu.raft.protocol.Heartbeat;
import com.baidu.raft.protocol.VoteRQ;
import com.baidu.raft.protocol.VoteRS;
import com.baidu.raft.storage.LogStorage;
import com.baidu.raft.utils.JSONUtil;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by pippo on 16/7/29.
 */
public class ElectionStat {

    private Logger logger = LoggerFactory.getLogger(ElectionStat.class);

    public static ElectionStat get() {
        return stat;
    }

    static final ElectionStat stat = new ElectionStat();

    public <T> T process(ElectionStatProcessor<T> processor) {

        // TODO unsync support
        synchronized (this) {
            return processor.process(this);
        }
    }

    // 初始化
    public ElectionStat init(int currentId) {
        this.currentId = currentId;

        // 1.设置当前角色为follower
        currentRole = PeerRole.Follower;
        leader = -1;

        // 2.设置当前term为0
        currentTerm.set(0);
        acceptCount.set(0);

        // 读取本地持久化的状态(如果有)
        retrieve();

        // 3.启动electionTimer(随机时间后run)
        // 4.如果electionTimer真正run的时候还没有收到leader的心跳,那么就发起选举,否则取消选举任务
        startElection();

        return this;
    }

    // 发起投票
    public ElectionStat vote() {
        return process(new VoteProcessor());
    }

    public void process(Heartbeat heartbeat) {
        process(new HeartbeatProcessor(heartbeat));
    }

    public void process(VoteRQ rq) {
        process(new VoteRQProcessor(rq));
    }

    public void process(VoteRS rs) {
        process(new VoteRSProcessor(rs));
    }

    public boolean changeRole(long term, PeerRole role) {
        return process(new RoleChanger(term, role));
    }

    public boolean isVoteComplete() {
        return leader > 0;
    }

    public boolean isLeader() {
        return leader == currentId;
    }

    public boolean hasLeader() {
        return leader > 0;
    }

    public int getCurrentId() {
        return currentId;
    }

    public PeerRole getCurrentRole() {
        return currentRole;
    }

    public int getLeader() {
        return leader;
    }

    public long getCurrentTerm() {
        return currentTerm.get();
    }

    int currentId = -1;
    volatile PeerRole currentRole = PeerRole.Observer;
    volatile int leader = -1;
    volatile int voteTo = -1;
    AtomicLong currentTerm = new AtomicLong(0);
    AtomicInteger acceptCount = new AtomicInteger(0);
    RandomTimer electionTimer = new RandomTimer("election");

    boolean setLeader(long term, int leader) {
        boolean flag = true;
        long oldTerm = currentTerm.get();
        if (oldTerm < term) {
            flag = currentTerm.compareAndSet(oldTerm, term);
        }

        if (flag) {
            this.leader = leader;
            this.voteTo = leader;
        }

        persist();
        return flag;
    }

    void startElection() {
        electionTimer.start(new RandomTimer.TimerTask() {

            @Override
            public boolean doNext() {
                if (isVoteComplete()) {
                    logger.info("the election is complete, cancel election task");
                    return false;
                } else {
                    return true;
                }
            }

            @Override
            public void run() {
                vote();
            }
        });
    }

    void persist() {
        Path path = FileSystems.getDefault().getPath(PeerMain.config.workDir, String.valueOf(currentId),
                "election.stat");

        try {
            if (!path.toFile().exists()) {
                Files.createDirectories(path.getParent());
                Files.createFile(path);
            }

            String stat = String.format("{'term':%s, 'voteTo':%s}", currentTerm.get(), voteTo);
            Files.write(path, stat.getBytes());
        } catch (IOException e) {
            logger.error("persist stat due to error", e);
        }
    }

    void retrieve() {
        Path path = FileSystems.getDefault().getPath(PeerMain.config.workDir, String.valueOf(currentId),
                "election.stat");
        if (!path.toFile().exists()) {
            return;
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> stat = JSONUtil.toObject(Files.readAllBytes(path), Map.class);
            currentTerm.set(((Number) stat.get("term")).longValue());
            // voteTo = (int) stat.get("voteTo");
        } catch (IOException e) {
            logger.error("retrieve stat due to error", e);
        }
    }

    void startHeartbeat() {
        electionTimer.stop();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("currentId", currentId)
                .append("currentRole", currentRole)
                .append("leader", leader)
                .append("voteTo", voteTo)
                .append("currentTerm", currentTerm)
                .append("acceptCount", acceptCount)
                .toString();
    }

    private class VoteProcessor implements ElectionStatProcessor<ElectionStat> {

        @Override
        public ElectionStat process(ElectionStat stat) {
            // 1.当前角色为candidate
            currentRole = PeerRole.Candidate;
            leader = -1;

            // 2.增加term
            long term = currentTerm.incrementAndGet();

            // 3.投票给自己
            voteTo = currentId;
            acceptCount.set(1);

            // 4.向其他节点发送投票请求
            try {
                RaftCluster cluster = RaftCluster.get();
                LogStorage logStorage = cluster.getLogStorage();

                VoteRQ rq = new VoteRQ(stat.getCurrentId(),
                        -1,
                        term,
                        logStorage.getLastIndex(),
                        logStorage.getLast().getTerm(),
                        currentId);

                cluster.broadcast(rq);
            } catch (Exception e) {
                logger.error("broad cast vote rq due to error", e);
            }

            return stat;
        }
    }

    private class RoleChanger implements ElectionStatProcessor<Boolean> {

        private RoleChanger(long term, PeerRole target) {
            this.term = term;
            this.target = target;
        }

        private long term;
        private PeerRole target;

        @Override
        public Boolean process(ElectionStat stat) {
            if (currentRole != PeerRole.Leader && target == PeerRole.Leader) {
                // 当前node不是leader,将成为leader
                // 停止选举daemon,开始心跳daemon
                if (setLeader(term, currentId)) {
                    startHeartbeat();
                }
            } else if (currentRole == PeerRole.Leader || target == PeerRole.Candidate) {
                // 当前node是leader,将失去leader
                // 停止心跳daemon,开始选举daemon
                setLeader(term, -1);
                startElection();
            }

            currentRole = target;
            return true;
        }

    }

}

package com.baidu.raft.election;

import com.baidu.raft.RaftCluster;
import com.baidu.raft.protocol.VoteRQ;
import com.baidu.raft.protocol.VoteRS;
import com.baidu.raft.storage.LogStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by pippo on 16/8/2.
 */
public class VoteRQProcessor implements ElectionStatProcessor<Boolean> {

    private static Logger logger = LoggerFactory.getLogger(VoteRQProcessor.class);

    public VoteRQProcessor(VoteRQ rq) {
        this.rq = rq;
    }

    private VoteRQ rq;

    @Override
    public Boolean process(ElectionStat stat) {
        boolean accept = validate(stat);
        RaftCluster.get().send(rq.source, new VoteRS(stat.currentId, rq.source, rq.term, accept));
        return accept;
    }

    private Boolean validate(ElectionStat stat) {
        long term = stat.currentTerm.get();

        // 如果投票请求的term和当前term不符,那么直接忽略该投票请求
        if (rq.term != term) {

            logger.info("forbid vote rq:[{}] witch not equal current term:[{}]",
                    rq,
                    term);

            // 如果当前节点term小于rq,重置当前节点term
            return false;
        }

        // 如果已经有leader了还收到rq请求,那么拒绝
        if (stat.leader > 0) {
            logger.info("forbid vote rq:[{}] because current has leader:[{}]", rq, stat);
            return false;
        }

        if (stat.voteTo > 0 && stat.voteTo != rq.leader) {
            logger.info("forbid vote rq:[{}] because current has vote to:[{}]", rq, stat);
            return false;
        }

        LogStorage storage = RaftCluster.get().getLogStorage();
        long lastIndex = storage.getLastIndex();
        long lastTerm = storage.getLast().getTerm();

        if (rq.lastIndex >= lastIndex && rq.lastTerm >= lastTerm) {
            logger.info("accept vote rq:[{}]", rq);
            return true;
        } else {
            logger.info("forbid vote rq:[{}] because current log:[{}/{}] more up to date", rq, lastIndex, lastTerm);
            return false;
        }

    }
}

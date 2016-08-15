package com.baidu.raft.protocol;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * Created by pippo on 16/7/25.
 */
public class VoteRQ extends BaseMessage {

    public VoteRQ() {

    }

    public VoteRQ(int source,
            int destination,
            long term,
            long lastIndex,
            long lastTerm,
            int leader) {
        super(MessageType.VoteRQ, source, destination, term);
        this.lastIndex = lastIndex;
        this.lastTerm = lastTerm;
        this.leader = leader;
    }

    public long lastIndex;
    public long lastTerm;
    public int leader;

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .appendSuper(super.toString())
                .append("lastIndex", lastIndex)
                .append("lastTerm", lastTerm)
                .append("leader", leader)
                .toString();
    }
}

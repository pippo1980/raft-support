package com.baidu.raft.protocol;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * Created by pippo on 16/7/26.
 */
public class Heartbeat extends BaseMessage {

    public Heartbeat() {
    }

    public Heartbeat(int source, int destination, long term, int leader) {
        super(MessageType.Heartbeat, source, destination, term);
        this.leader = leader;
    }

    public int leader;

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .appendSuper(super.toString())
                .append("leader", leader)
                .toString();
    }
}

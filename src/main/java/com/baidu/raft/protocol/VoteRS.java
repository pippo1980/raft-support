package com.baidu.raft.protocol;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * Created by pippo on 16/7/25.
 */
public class VoteRS extends BaseMessage {

    public VoteRS() {
    }

    public VoteRS(int source, int destination, long term, boolean accept) {
        super(MessageType.VoteRS, source, destination, term);
        this.accept = accept;
    }

    public boolean accept;

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .appendSuper(super.toString())
                .append("accept", accept)
                .toString();
    }
}

package com.baidu.raft.protocol;

import com.baidu.raft.PeerNode;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.Collection;
import java.util.HashSet;

/**
 * Created by pippo on 16/8/4.
 */
public class JoinClusterRS extends BaseMessage {

    public JoinClusterRS() {
    }

    public JoinClusterRS(int source,
            int destination,
            long term,
            Collection<PeerNode> nodes) {
        super(MessageType.JoinClusterRS, source, destination, term);
        this.nodes = nodes;
    }

    public Collection<PeerNode> nodes = new HashSet<>();

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .appendSuper(super.toString())
                .append("nodes", nodes)
                .toString();
    }
}

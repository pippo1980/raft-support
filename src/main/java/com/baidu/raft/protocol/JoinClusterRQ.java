package com.baidu.raft.protocol;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * Created by pippo on 16/8/4.
 */
public class JoinClusterRQ extends BaseMessage {

    public JoinClusterRQ() {
    }

    public JoinClusterRQ(int source, int destination, long term, int id, String host, int port) {
        super(MessageType.JoinClusterRQ, source, destination, term);
        this.id = id;
        this.host = host;
        this.port = port;
    }

    public int id;
    public String host;
    public int port;

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .appendSuper(super.toString())
                .append("id", id)
                .append("host", host)
                .append("port", port)
                .toString();
    }
}

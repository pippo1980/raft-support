package com.baidu.raft;

import com.baidu.raft.endpoint.tcp.RaftClient;
import com.baidu.raft.protocol.BaseMessage;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Created by pippo on 16/7/25.
 */
public class PeerNode {

    private static Logger logger = LoggerFactory.getLogger(PeerNode.class);

    public PeerNode() {

    }

    public PeerNode(int id, String host, int port) {
        this.id = id;
        this.host = host;
        this.port = port;
    }

    public void send(BaseMessage message) {
        try {
            initClient().write(message);
        } catch (Exception e) {
            logger.error("send message:[{}] to node:[{}:{}] due to error:[{}]",
                    message.type,
                    host,
                    port,
                    ExceptionUtils.getStackTrace(e));
        }
    }

    public boolean isActive() {
        return client != null && client.isActive();
    }

    protected RaftClient initClient() {
        if (client == null) {
            client = new RaftClient(host, port);
        }

        try {
            client.open();
        } catch (Exception e) {
            // do nothing
        }

        return client;
    }

    protected int id;
    protected String host;
    protected int port;
    protected RaftClient client;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("id", id)
                .append("host", host)
                .append("port", port)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PeerNode node = (PeerNode) o;
        return id == node.id &&
               port == node.port &&
               Objects.equals(host, node.host);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, host, port);
    }
}

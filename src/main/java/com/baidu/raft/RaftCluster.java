package com.baidu.raft;

import com.baidu.raft.election.ElectionStat;
import com.baidu.raft.protocol.BaseMessage;
import com.baidu.raft.protocol.JoinClusterRQ;
import com.baidu.raft.protocol.JoinClusterRS;
import com.baidu.raft.storage.LogStorage;
import com.baidu.raft.utils.JSONUtil;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by pippo on 16/7/25.
 */
public class RaftCluster {

    private static Logger logger = LoggerFactory.getLogger(RaftCluster.class);

    private static RaftCluster cluster;

    public static RaftCluster get() {
        return cluster;
    }

    RaftCluster(int currentId, String currentHost, int currentPort, LogStorage logStorage) {
        this.currentId = currentId;
        this.currentHost = currentHost;
        this.currentPort = currentPort;
        this.logStorage = logStorage;
        this.retrieve();
        this.init();

        cluster = this;
    }

    final int currentId;
    final String currentHost;
    final int currentPort;
    final LogStorage logStorage;
    final Map<Integer, PeerNode> nodes = new ConcurrentHashMap<>();

    public int getCurrentId() {
        return currentId;
    }

    public String getCurrentHost() {
        return currentHost;
    }

    public int getCurrentPort() {
        return currentPort;
    }

    public LogStorage getLogStorage() {
        return logStorage;
    }

    public void setNodes(Set<PeerNode> nodes) {
        for (PeerNode node : nodes) {
            node.initClient();
            this.nodes.put(node.id, node);
        }
    }

    public void init() {
        for (PeerNode node : nodes.values()) {
            try {
                node.initClient();
            } catch (Exception e) {
                // do nothing
            }
        }
    }

    public int getAcceptThreshold() {
        int count = 1;

        for (PeerNode node : nodes.values()) {
            if (node.isActive()) {
                count++;
            }
        }

        logger.debug("current node:[{}] consider active node size is:[{}]", currentId, count);
        return count;
    }

    public void broadcast(BaseMessage message) {
        for (Integer id : nodes.keySet()) {
            send(id, (BaseMessage) message.clone());
        }
    }

    public void send(int id, BaseMessage message) {
        if (id == message.source) {
            return;
        }

        PeerNode node = nodes.get(id);
        if (node != null) {
            message.destination = id;
            node.send(message);
        }
    }

    public boolean exist(int id) {
        return nodes.containsKey(id);
    }

    public void join(JoinClusterRQ rq) {
        join(new PeerNode(rq.id, rq.host, rq.port));
    }

    public void join(PeerNode node) {
        if (!nodes.containsKey(node.id)) {
            node = new PeerNode(node.id, node.host, node.port);
            node.initClient();
            nodes.put(node.id, node);
        } else {
            PeerNode exists = nodes.get(node.id);
            exists.host = node.host;
            exists.port = node.port;
        }

        logger.info("cluster nodes is:[{}]", nodes);

        // 返回当前node的cluster nodes
        node.send(new JoinClusterRS(currentId, node.id, ElectionStat.get().getCurrentTerm(), nodes.values()));

        // 持久化
        persist();
    }

    public void update(JoinClusterRS message) {
        for (PeerNode node : message.nodes) {
            if (!nodes.containsKey(node.id)) {
                node.initClient();
                nodes.put(node.id, node);
            } else {
                PeerNode exists = nodes.get(node.id);
                exists.host = node.host;
                exists.port = node.port;
            }
        }

        logger.info("cluster nodes is:[{}]", nodes);

        // 持久化
        persist();
    }

    public void leave(PeerNode node) {
        nodes.remove(node.id);
    }

    public PeerNode randomNode(int... exclude) {
        List<PeerNode> nodes = new ArrayList<>();

        for (PeerNode node : this.nodes.values()) {
            if (!ArrayUtils.contains(exclude, node.id)) {
                nodes.add(node);
            }
        }

        return nodes.isEmpty() ? null : nodes.get(new Random().nextInt(nodes.size()));
    }

    void persist() {
        Path path = FileSystems.getDefault().getPath(PeerMain.config.workDir, String.valueOf(currentId),
                "nodes");

        try {
            if (!path.toFile().exists()) {
                Files.createDirectories(path.getParent());
                Files.createFile(path);
            }

            Files.write(path, JSONUtil.toBytes(nodes.values()));
        } catch (IOException e) {
            logger.error("persist stat due to error", e);
        }
    }

    void retrieve() {
        Path path = FileSystems.getDefault().getPath(PeerMain.config.workDir, String.valueOf(currentId),
                "nodes");
        if (!path.toFile().exists()) {
            return;
        }

        try {
            PeerNode[] nodes = JSONUtil.toObject(Files.readAllBytes(path), PeerNode[].class);
            for (PeerNode node : nodes) {
                this.nodes.put(node.id, node);
            }
        } catch (IOException e) {
            logger.error("retrieve stat due to error", e);
        }
    }

}

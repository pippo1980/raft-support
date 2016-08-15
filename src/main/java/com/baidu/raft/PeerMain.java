package com.baidu.raft;

import com.baidu.raft.election.ElectionStat;
import com.baidu.raft.endpoint.tcp.RaftEndpoint;
import com.baidu.raft.endpoint.tcp.RaftMessageHandler;
import com.baidu.raft.storage.LevelDBLogStorage;
import com.baidu.raft.storage.LogEntry;
import com.baidu.raft.storage.LogStorage;
import com.baidu.raft.storage.LogType;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by pippo on 16/7/27.
 */
public class PeerMain {

    private static Logger logger = LoggerFactory.getLogger(PeerMain.class);

    public static Config config = Config.load();
    public static RaftEndpoint endpoint = null;

    static int id;
    static String host;
    static int port;

    public static void main(String[] args) throws Exception {
        id = NumberUtils.toInt(args[0]);
        host = StringUtils.defaultString(args[1], "0.0.0.0");
        port = NumberUtils.toInt(args[2], 8964);

        start(parseConf());

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            stop();
        }));

        Thread.currentThread().join();
    }

    public static PeerNode parseConf() throws IOException {
        Set<PeerNode> nodes = new HashSet<>();

        PeerNode current = new PeerNode();
        current.id = id;
        current.host = host;
        current.port = port;
        nodes.add(current);

        for (Config.Seed seed : config.seeds) {

            try {
                PeerNode node = new PeerNode();
                node.id = seed.id;
                node.host = seed.host;
                node.port = seed.port;
                nodes.add(node);
            } catch (Exception e) {

            }
        }

        RaftCluster cluster = new RaftCluster(current.id,
                current.host,
                current.port,
                new LevelDBLogStorage(config.workDir, id));
        cluster.setNodes(nodes);

        LogStorage logStorage = cluster.getLogStorage();
        if (logStorage.getLastIndex() <= 0) {
            logStorage.append(new LogEntry(LogType.Application, ElectionStat.get().getCurrentTerm(), null));
        }

        ElectionStat.get().init(current.id);
        return current;
    }

    public static void start(PeerNode current) throws InterruptedException {
        endpoint = new RaftEndpoint(current.host,
                current.port,
                new RaftMessageHandler());
        endpoint.start();
    }

    public static void stop() {
        if (endpoint != null) {
            endpoint.stop();
        }
    }

}

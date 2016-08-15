package com.baidu.raft.election;

import com.baidu.raft.RaftCluster;
import com.baidu.raft.protocol.Heartbeat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Created by pippo on 16/7/27.
 */
public class HeartbeatTimer {

    private static Logger logger = LoggerFactory.getLogger(HeartbeatTimer.class);
    private static ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    private static long heartbeatInterval = 50;

    private ScheduledFuture<?> heartbeatFuture;

    public void start() {
        if (heartbeatFuture == null || heartbeatFuture.isCancelled()) {
            logger.info("start heartbeat task");
            heartbeatFuture = executor.scheduleWithFixedDelay(new HeartbeatTask(),
                    0,
                    heartbeatInterval,
                    TimeUnit.MILLISECONDS);
        }
    }

    public void stop() {
        if (heartbeatFuture != null) {
            logger.info("stop heartbeat task");
            heartbeatFuture.cancel(true);
            heartbeatFuture = null;
        }
    }

    private void send() {
        Heartbeat heartbeat = ElectionStat.get().process(stat -> {
            return new Heartbeat(stat.getCurrentId(),
                    -1,
                    stat.getCurrentTerm(),
                    stat.getLeader());
        });

        RaftCluster.get().broadcast(heartbeat);
    }

    private class HeartbeatTask implements Runnable {

        public void run() {

            if (!ElectionStat.get().isLeader()) {
                logger.info("current node:[{}] is not leader");
                heartbeatFuture.cancel(true);
                return;
            }

            try {
                send();
            } catch (Exception e) {
                logger.error("send heartbeat due to error", e);
            }
        }

    }
}

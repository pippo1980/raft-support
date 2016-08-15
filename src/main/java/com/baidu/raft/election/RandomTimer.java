package com.baidu.raft.election;

import org.apache.commons.lang.math.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Created by pippo on 16/7/27.
 */
public class RandomTimer {

    private static Logger logger = LoggerFactory.getLogger(RandomTimer.class);
    private static ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

    public RandomTimer(String name) {
        this.name = name;
    }

    public RandomTimer(String name, long minInterval, long maxInterval) {
        this.name = name;
        this.minInterval = minInterval;
        this.maxInterval = maxInterval;
    }

    private String name;
    private long minInterval = 100;
    private long maxInterval = 300;
    private HeartbeatTimer heartbeatTimer = new HeartbeatTimer();
    private volatile ScheduledFuture<?> electionFuture;

    public void start(TimerTask task) {
        if (electionFuture != null) {
            return;
        }

        logger.info("start random timer task:[{}]", name);
        doStart(task);
    }

    public void stop() {

        if (electionFuture != null) {
            logger.info("stop random timer task:[{}]", name);
            electionFuture.cancel(true);
            electionFuture = null;
        }

        if (ElectionStat.get().isLeader()) {
            heartbeatTimer.start();
        }
    }

    private void doStart(TimerTask task) {
        heartbeatTimer.stop();
        electionFuture = executor.schedule(() -> {

            if (task.doNext()) {
                task.run();
                doStart(task);
            } else {
                electionFuture = null;
            }

        }, randomInterval(), TimeUnit.MILLISECONDS);
    }

    private long randomInterval() {
        return (RandomUtils.nextInt((int) (maxInterval - minInterval)) + minInterval);
    }

    public interface TimerTask {

        boolean doNext();

        void run();

    }

}

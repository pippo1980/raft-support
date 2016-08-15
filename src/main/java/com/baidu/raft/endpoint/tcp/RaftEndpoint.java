package com.baidu.raft.endpoint.tcp;

import com.baidu.raft.RaftCluster;
import com.baidu.raft.election.ElectionStat;
import com.baidu.raft.protocol.JoinClusterRQ;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

/**
 * Created by pippo on 16/7/27.
 */
public class RaftEndpoint {

    private static Logger logger = LoggerFactory.getLogger(RaftEndpoint.class);

    public RaftEndpoint(ChannelHandler channelHandler) {
        this.channelHandler = channelHandler;
    }

    public RaftEndpoint(String host, int port, ChannelHandler channelHandler) {
        this.host = host;
        this.port = port;
        this.channelHandler = channelHandler;
    }

    public RaftEndpoint(String host,
            int port,
            int connectTimeout,
            int soBacklog,
            int soLinger,
            boolean soReuseaddr,
            int soTimeout, boolean nodelay, ChannelHandler channelHandler) {
        this.host = host;
        this.port = port;
        this.connectTimeout = connectTimeout;
        this.soBacklog = soBacklog;
        this.soLinger = soLinger;
        this.soReuseaddr = soReuseaddr;
        this.soTimeout = soTimeout;
        this.nodelay = nodelay;
        this.channelHandler = channelHandler;
    }

    private Semaphore monitor = new Semaphore(0);
    private ExecutorService daemon = null;

    public void start() throws InterruptedException {
        daemon = Executors.newSingleThreadExecutor();
        daemon.execute(new DaemonTask());
        monitor.acquire();
    }

    public void stop() {
        if (daemon != null) {
            daemon.shutdown();
        }
    }

    private String host = "0.0.0.0";
    private int port = 8156;
    private int connectTimeout = 100;
    private int soBacklog = 100;
    private int soLinger = 100;
    private boolean soReuseaddr = true;
    private int soTimeout = 100;
    private boolean nodelay = true;
    private ChannelHandler channelHandler;

    private class DaemonTask implements Runnable {

        private ServerBootstrap bootstrap = new ServerBootstrap();
        private EventLoopGroup accepter;
        private EventLoopGroup processor;

        @Override
        public void run() {
            try {
                bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout)
                        .option(ChannelOption.SO_BACKLOG, soBacklog)
                        .option(ChannelOption.SO_LINGER, soLinger)
                        .option(ChannelOption.SO_REUSEADDR, soReuseaddr)
                        .option(ChannelOption.SO_TIMEOUT, soTimeout)
                        .option(ChannelOption.TCP_NODELAY, nodelay);

                /* 线程池 */
                accepter = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors());
                processor = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors() * 4);
                bootstrap.group(accepter, processor).channel(NioServerSocketChannel.class);

                /* init event process stream */
                bootstrap.childHandler(new CustomEndpointInitializer(channelHandler));

                /* Start the server */
                ChannelFuture f = bootstrap.bind(host, port).addListener(new EndpointOpenListener()).sync();
                /* Wait until the server socket is closed */
                f.channel().closeFuture().addListener(new EndpointCloseListener(this)).sync();
            } catch (Exception e) {
                logger.error("the tcp endpoint due to error:[{}], will close endpoint", ExceptionUtils.getStackTrace(e));
            }
        }

        public void stop() {

            logger.info("勒个去tcp endpoint要关闭了,回家收衣服啊!!!");

            if (accepter != null) {
                accepter.shutdownGracefully();
            }

            if (processor != null) {
                processor.shutdownGracefully();
            }
        }
    }

    private class EndpointOpenListener implements GenericFutureListener<Future<? super Void>> {

        @Override
        public void operationComplete(Future<? super Void> future) throws Exception {
            logger.info("open tcp endpoint on address:[{}:{}]", host, port);
            monitor.release();

            // send node join
            ElectionStat stat = ElectionStat.get();
            RaftCluster cluster = RaftCluster.get();
            cluster.broadcast(new JoinClusterRQ(stat.getCurrentId(),
                    -1,
                    stat.getCurrentTerm(),
                    cluster.getCurrentId(),
                    cluster.getCurrentHost(),
                    cluster.getCurrentPort()));
        }
    }

    private class EndpointCloseListener implements GenericFutureListener<Future<? super Void>> {

        public EndpointCloseListener(DaemonTask daemonTask) {
            this.daemonTask = daemonTask;
        }

        DaemonTask daemonTask;

        @Override
        public void operationComplete(Future<? super Void> future) throws Exception {
            daemonTask.stop();

            // TODO send node leave
        }
    }

}

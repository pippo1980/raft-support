package com.baidu.raft.endpoint.tcp;

import com.baidu.raft.protocol.BaseMessage;
import com.baidu.raft.protocol.Heartbeat;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Created by pippo on 15/12/21.
 */
public class RaftClient implements Closeable {

    private static Logger logger = LoggerFactory.getLogger(RaftClient.class);

    public RaftClient() {

    }

    public RaftClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    private String host = "0.0.0.0";
    private int port = 8256;

    private Connection connection = new Connection();

    private ExecutorService daemon = Executors.newSingleThreadExecutor();

    public boolean open() throws Exception {
        if (isActive()) {
            return true;
        }

        daemon.execute(connection);
        try {
            connection.started.tryAcquire(10, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {

        }

        return isActive();
    }

    @Override
    public void close() {
        daemon.shutdown();

        if (isActive()) {
            connection.ctx.close();
            connection.ctx = null;
            connection = null;
        }

        logger.info("close raft client:[{}]", this);
    }

    public boolean isActive() {
        return connection != null && connection.ctx != null && connection.ctx.channel().isActive();
    }

    public void setCallback(MessageCallback callback) {
        connection.callback = callback;
    }

    public void write(BaseMessage msg) throws Exception {
        if (!open()) {
            logger.trace("the connection to [{}:{}] not open, ignore write message", host, port);
            return;
        }

        if (!(msg instanceof Heartbeat)) {
            logger.debug("write message:[{}] to node:[{}:{}]", msg, host, port);
        }

        connection.ctx.writeAndFlush(msg);
    }

    public interface MessageCallback {

        void onMessage(Connection connection, Object msg);

    }

    public class Connection implements Runnable {

        public Connection() {
            EventLoopGroup group = new NioEventLoopGroup(2);
            bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .handler(new CustomEndpointInitializer(new ClientMessageHandler(this)));
        }

        private Semaphore started = new Semaphore(0);
        private Bootstrap bootstrap;
        private ChannelHandlerContext ctx;
        private MessageCallback callback;

        @Override
        public void run() {
            try {
                // Start the client.
                logger.trace("try to connect node:[{}:{}]", host, port);
                ChannelFuture f = bootstrap.connect(host, port).sync();

                // Wait until the connection is closed.
                f.channel().closeFuture().sync();
            } catch (Exception e) {
                logger.trace("try to connect node:[{}:{}] due to error:[{}]", host, port, e.getMessage());
            } finally {
                // Shut down the event loop to terminate all threads.
            }
        }

    }

    @ChannelHandler.Sharable
    private static class ClientMessageHandler extends ChannelDuplexHandler {

        public ClientMessageHandler(Connection connection) {
            this.connection = connection;
        }

        private Connection connection;

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            connection.ctx = ctx;
            connection.started.release();
            logger.trace("connect to node:[{}]", ctx.channel().remoteAddress());
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            try {
                if (connection.callback != null) {
                    onMessage(ctx, msg);
                }
            } catch (Exception e) {
                ctx.fireExceptionCaught(e);
            }
        }

        private void onMessage(ChannelHandlerContext ctx, Object msg) {
            connection.callback.onMessage(connection, msg);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            connection.ctx = null;
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            // Close the connection when an exception is raised.
            // logger.log(Level.WARNING, "Unexpected exception from downstream.", cause);
            cause.printStackTrace();
            ctx.close();
        }

    }

}


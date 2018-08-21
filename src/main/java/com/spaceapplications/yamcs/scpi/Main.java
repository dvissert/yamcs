package com.spaceapplications.yamcs.scpi;

import static pl.touk.throwing.ThrowingSupplier.unchecked;

import java.util.Optional;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public class Main {
  public static Integer DEFAULT_PORT = 1337;
  public static Integer DEFAULT_MAX_CONNECTIONS = 5;

  public static void main(String[] args) {
    new Main(Args.parse(args));
  }

  public Main(Args args) {
    // No null checking for args.config as it is an obligated arg.
    Config config = Config.load(args.config); 
    int port = Optional.ofNullable(config.daemon).map(d -> d.port).orElse(DEFAULT_PORT);

    NioEventLoopGroup boss = new NioEventLoopGroup();
    NioEventLoopGroup worker = new NioEventLoopGroup();

    Commander commander = new Commander(config);
    ServerHandler handler = new ServerHandler(commander);
    ServerInitializer init = new ServerInitializer(handler);

    try {
      bootstrap(boss, worker, init, port);
    } finally {
      boss.shutdownGracefully();
      worker.shutdownGracefully();
    }
  }

  private void bootstrap(NioEventLoopGroup boss, NioEventLoopGroup worker, ServerInitializer initializer, int port) {
    ChannelFuture f = new ServerBootstrap().group(boss, worker).channel(NioServerSocketChannel.class)
        .option(ChannelOption.SO_BACKLOG, DEFAULT_MAX_CONNECTIONS).handler(new LoggingHandler(LogLevel.INFO))
        .childHandler(initializer).bind(port);
    blockUntilInterrupted(f);
  }

  private void blockUntilInterrupted(ChannelFuture channelFuture) {
    channelFuture = unchecked(channelFuture::sync).get();
    unchecked(channelFuture.channel().closeFuture()::sync).get();
  }
}
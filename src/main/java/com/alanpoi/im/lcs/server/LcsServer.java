package com.alanpoi.im.lcs.server;

import com.alanpoi.im.lcs.secprotocol.SecpHandler;
import com.alanpoi.im.lcs.secprotocol.SecpMsgDecoder;
import com.alanpoi.im.lcs.secprotocol.SecpMsgEncoder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Component
public class LcsServer{
	private static final Logger log = LoggerFactory.getLogger(LcsServer.class);
	
	@Value("${server.secp.port}")
	private int port;

	@Autowired private SecpHandler messageHandler;

	private HandlerInitalizer handlerInitalizer;
	private Channel serverChnl;
	private EventLoopGroup bossGroup;
	private EventLoopGroup workerGroup;

	public LcsServer(){
		bossGroup = new NioEventLoopGroup(8);
		workerGroup = new NioEventLoopGroup(8);
		handlerInitalizer = new HandlerInitalizer();
	}

	@PostConstruct
	public void run(){
		log.info("begin start lcs server");
		
		try{
			ServerBootstrap  b = new ServerBootstrap();
			b.group(bossGroup, workerGroup)
				.channel(NioServerSocketChannel.class)
				.childHandler(handlerInitalizer)
				.option(ChannelOption.SO_BACKLOG, 1024)
					.option(ChannelOption.SO_REUSEADDR, true)
			//.childOption(ChannelOption.SO_KEEPALIVE, true)
			;

			ChannelFuture f;
			f = b.bind(port).sync();
			serverChnl = f.channel();

			log.info("lcs server listen at: {}", port);

		}catch(Exception e){
			log.error("lcs server error: {}",port, e);
		}
	}

	@PreDestroy
	public void stop(){
		serverChnl.close();
		bossGroup.shutdownGracefully();
		workerGroup.shutdownGracefully();
	}

	private class HandlerInitalizer extends ChannelInitializer<SocketChannel> {

		private DefaultEventExecutorGroup executor = null;

		public HandlerInitalizer(){
			executor = new DefaultEventExecutorGroup(16, new DefaultThreadFactory("secp"));
		}

		@Override
		protected void initChannel(SocketChannel ch) throws Exception {
			// TODO Auto-generated method stub
			//log.info("initChannel");

			ch.pipeline().addLast(new SecpMsgDecoder());
			ch.pipeline().addLast(new SecpMsgEncoder());
			ch.pipeline().addLast(executor, messageHandler);
		}

	}
}

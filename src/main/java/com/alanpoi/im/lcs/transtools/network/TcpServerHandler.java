package com.alanpoi.im.lcs.transtools.network;


import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author brandon
 * @create 2019-07-18
 *
 * Server端handler
 */
public class TcpServerHandler extends SimpleChannelInboundHandler<Frame> {
    private static final Logger log = LoggerFactory.getLogger(TcpServer.class);

    @Override
    protected void messageReceived(ChannelHandlerContext ctx, Frame msg) throws Exception {
        //log.debug("recv frame msg. cmd:{}", msg.getCmd());
        if(Frame.CMD_HEARTBEAT == msg.getCmd()){
            Frame res = msg.clone();
            res.setCmd(Frame.CMD_HEARTBEAT_RES);
            ctx.channel().writeAndFlush(res);
        }

        ctx.fireChannelRead(msg);
    }


}

package com.alanpoi.im.lcs.transtools.network;


import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;


/**
 * @author brandon
 * @create 2019-07-18
 *
 * 客户端handler
 */
public class TcpClientHandler extends SimpleChannelInboundHandler<Frame> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Frame msg) throws Exception {
        TcpClient client = ctx.channel().attr(TcpClient.CLIENT).get();
        if(null == client){
            ctx.fireChannelRead(msg);
            return;
        }

        /*
        if(msg.getCmd() == Frame.CMD_HEARTBEAT_RES){

        }*/
        if(msg.getCmd() == Frame.CMD_DATA_RES){
            client.onResponse(msg);
        }

        ctx.fireChannelRead(msg);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelActive();
    }


    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        TcpClient client = ctx.channel().attr(TcpClient.CLIENT).get();
        if(null == client){
            ctx.fireChannelInactive();
            return;
        }

        client.onInactive();
        ctx.fireChannelInactive();
    }

    /*
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        ctx.fireExceptionCaught(cause);
    }
    */

}

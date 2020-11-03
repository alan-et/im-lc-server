package com.alanpoi.im.lcs.secprotocol;

import com.alanpoi.im.lcs.imsignal.SignalHandler;
import com.alanpoi.im.lcs.metrics.Counters;
import com.alanpoi.im.lcs.secprotocol.channel.SecpChannel;
import com.alanpoi.im.lcs.secprotocol.channel.SecpChannelAttrs;
import com.alanpoi.im.lcs.util.EncryptRSA;
import com.alanpoi.im.lcs.util.IPUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.InetAddress;
import java.net.InetSocketAddress;


/**
 * create by brandon
 * 2019-02-28
 *Secp协议处理器
 *
 */

@Component
@ChannelHandler.Sharable
public class SecpHandler extends SimpleChannelInboundHandler<SecpMessage> {
	private static final Logger log = LoggerFactory.getLogger(SecpHandler.class);

	@Autowired private SecpChannelManager channelMgr;
	@Autowired private SignalHandler sigHandler;
	@Autowired
	private EncryptRSA rsa;
	@Autowired
	private LcSessionManager lcsionMgr;

	@Autowired
	private Counters counters;

	//创建连接
	@Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
		Channel chnl = ctx.channel();

		DefaultSecpChannel secpChnl = new DefaultSecpChannel(chnl);
		chnl.attr(SecpChannelAttrs.SECPCHANNEL).set(secpChnl);
		chnl.attr(SecpChannelAttrs.CLOSREASON).set("by remote");

		channelMgr.add(secpChnl);

		InetSocketAddress addr = (InetSocketAddress)chnl.remoteAddress();
		InetAddress address = ((InetSocketAddress) chnl.remoteAddress()).getAddress();
		if (!IPUtil.isAliHealthIp(address.getAddress())) {
			log.info("socket channel created channelId: {}, ip:{}", secpChnl.getId(), addr.getHostName());
			counters.increment(Counters.SECP_CUSTOM_CONNECTED);
		}

	}
	
	@Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		Channel chnl = ctx.channel();
		InetSocketAddress addr = (InetSocketAddress)chnl.remoteAddress();
		SecpChannel secpChnl = chnl.attr(SecpChannelAttrs.SECPCHANNEL).get();
		long chnlId = 0;
		long sessionId = 0;
		if(null != secpChnl){
			chnlId = secpChnl.getId();
			channelMgr.closed(secpChnl);
			sigHandler.channelClosed(secpChnl);
		}
		String reason = chnl.attr(SecpChannelAttrs.CLOSREASON).get();
		InetAddress address = ((InetSocketAddress) chnl.remoteAddress()).getAddress();
		if (!IPUtil.isAliHealthIp(address.getAddress())) {
			log.info("channel closed. sessionId:{}, channelId:{}, ip:{}, reason:{}", sessionId, chnlId, addr.getHostName(), reason);
			counters.increment(Counters.SECP_CUSTOM_CLOSED);
		}

	}


	@Override
	protected void messageReceived(ChannelHandlerContext ctx, SecpMessage msg) throws Exception{
		Channel chnl = ctx.channel();
		DefaultSecpChannel secpChnl = (DefaultSecpChannel)chnl.attr(SecpChannelAttrs.SECPCHANNEL).get();
		log.info("recv msg lcId:{} channelId:{} cmd:{}", msg.getLcId(), secpChnl.getId(), msg.getCmd());

		if(null == secpChnl){
			log.error("miss secp channel");
			chnl.attr(SecpChannelAttrs.CLOSREASON).set("miss secp channel");
			chnl.close();
			return;
		}

		if(Cmd.CREATE_SECKEY_REQ == msg.getCmd()){
			initSecpChannel(secpChnl, msg);
			return;
		}

		if(msg.getLcId() != 0 && msg.getLcId() != secpChnl.getLcId()) {
		    long oldLcId = secpChnl.getLcId();
		    secpChnl.setLcId(msg.getLcId());
		    secpChnl.setSecKey(null);
		    log.info("lcId changed chnlId:{} lcId:{}->{}", secpChnl.getId(), oldLcId, secpChnl.getLcId());

            String secKey = null;
            if(secpChnl.getLcId() != 0){
                secKey = lcsionMgr.getSecKey(secpChnl.getLcId());
            }

            if(!StringUtils.isEmpty(secKey)){
                secpChnl.setSecKey(secKey);
                channelMgr.used(secpChnl);

                log.info("reload LCSession. chnlId:{} lcId:{} secKey len:{} md5:{}",
						secpChnl.getId(), secpChnl.getLcId(), secKey.length(), DigestUtils.md5Hex(secKey.getBytes()));
            }
        }

		if (StringUtils.isEmpty(secpChnl.getSecKey())
                && msg.getCmd() != Cmd.HEARTBEAT_REQ) //心跳可以不用加密
        {
            log.error("can't find secKey. chnlId:{} lcId:{}", secpChnl.getId(), secpChnl.getLcId());
            secpChnl.respondException(Errors.LCID_INVALID, msg);
            secpChnl.close("can't find secKey");
            return;
		}

		if(!secpChnl.decode(msg)){
			return;
		}

		switch(msg.getCmd()){
			case Cmd.TOS_SIGNAL_REQ:
				recvSignal(secpChnl, msg);
				break;
			case Cmd.HEARTBEAT_REQ:
				recvHeartbeat(secpChnl, msg);
				break;
		}

	}

	@Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
		ctx.flush();
    }
	
	@Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		/*com.alanpoi.im.lcs.net.SecpHandler.Event evn = new com.alanpoi.im.lcs.net.SecpHandler.Event(com.alanpoi.im.lcs.net.SecpHandler.Event.CHANNEL_EXCEPTION, ctx.channel());
		evn.setCause(cause);
		dispatcher.dispatch(evn);*/
		log.error("socket error:", cause);
    }

    //初始化安全通道
	private void initSecpChannel(DefaultSecpChannel secpChnl, SecpMessage msg) throws Exception{
		byte[] data = msg.getBody();
		byte[] aesKey = rsa.decryptByPrivateKey(data);
		String secKey = new String(aesKey);
		long lcId = lcsionMgr.newLcId();

		log.info("initSecpChannel channelId:{} lcId:{} secKey len:{} md5:{}",
				secpChnl.getId(), lcId, secKey.length(), DigestUtils.md5Hex(secKey.getBytes()));
		secpChnl.setLcId(lcId);
		secpChnl.setSecKey(secKey);
		lcsionMgr.saveSecKey(lcId, secKey);

		channelMgr.used(secpChnl);

		ByteBuf buf = secpChnl.getChannel().alloc().buffer();
		try {
			buf.writeLong(secpChnl.getLcId());
			secpChnl.respond(msg, buf);
		}finally {
			buf.release();
		}
	}
	
	//接收信令
	private void recvSignal(SecpChannel secpChnl, SecpMessage msg){

		sigHandler.recvSignal(secpChnl, msg);
	}
	//接收心跳
	private void recvHeartbeat(SecpChannel secpChnl, SecpMessage msg) throws Exception{
		secpChnl.setTimeStamp(System.currentTimeMillis());
		log.info("recv heartbeat channelId:{} ",secpChnl.getId());
		secpChnl.write(msg);
	}
	
}

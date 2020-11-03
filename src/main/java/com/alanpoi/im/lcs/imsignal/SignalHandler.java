package com.alanpoi.im.lcs.imsignal;

import com.alibaba.dubbo.config.annotation.Reference;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageLite;
import com.qzd.im.common.event2.EventProducer;
import com.qzd.im.common.response.CommonError;
import com.alanpoi.im.lcs.IMError;
import com.alanpoi.im.lcs.event.model.BackProcessEvent;
import com.alanpoi.im.lcs.event.model.ReportProcessEvent;
import com.alanpoi.im.lcs.event.model.SendMsgEvent;
import com.alanpoi.im.lcs.rabbitmq.RabbitPublisher;
import com.alanpoi.im.lcs.secprotocol.SecpMessage;
import com.alanpoi.im.lcs.secprotocol.channel.SecpChannel;
import com.alanpoi.im.lcs.util.NetworkUtil;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Objects;

//信令处理器
@Component
public class SignalHandler {
    private static final Logger log = LoggerFactory.getLogger(SignalHandler.class);
    public static final AttributeKey<UserChannel> USERCHANNEL = AttributeKey.valueOf("UserChannel");

    @Autowired
    private UserChannelManager userChnlMgrV2;

//    @Reference
//    private UserAuthorizedService authService;

    @Autowired
    private RabbitPublisher rabbitPublisher;
    @Autowired
    private EventProducer eventProducer;

    @Value("${qzdim.rabbitmq.lcs.queue.direct.internal:}")
    private String routingKey;

    private String localIP = NetworkUtil.getLocalIP();
    private ThreadLocal<SecpMessage> secpRequest = new ThreadLocal<>();


    public void recvSignal(SecpChannel secpChnl, SecpMessage msg) {
        secpRequest.set(msg);

        SignalProto.SignalRequest reqFull = null;
        SignalProto.ClientRequestHeader reqHeader = null;
        SignalProto.BindUserSig bindUserSig = null;

        try {
            reqFull = SignalProto.SignalRequest.parseFrom(msg.getBody());
            reqHeader = reqFull.getHeader();
            if (SignalProto.Cmd.BIND_USER_VALUE == reqHeader.getCmd() &&
                    reqFull.getBody() != null && reqFull.getBody().size() > 0) {
                bindUserSig = SignalProto.BindUserSig.parseFrom(reqFull.getBody());
            }
        } catch (InvalidProtocolBufferException e) {
            log.error("pare pb data error:", e);
            return;
        }

        //token鉴权
        //authToken(header.getUserId(), header.getToken());

        UserChannel uchnl = secpChnl.getChannel().attr(USERCHANNEL).get();
        if (null == uchnl && SignalProto.Cmd.BIND_USER_VALUE != reqHeader.getCmd()) {
            uchnl = bindUser(secpChnl, reqHeader, bindUserSig);
            if (null == uchnl) return;
        }

        try {
            switch (reqHeader.getCmd()) {
                case SignalProto.Cmd.BIND_USER_VALUE:
                    bindUser(secpChnl, reqHeader, bindUserSig);
                    break;
                case SignalProto.Cmd.SEND_MSG_VALUE:
                    SignalProto.SendMsgReq reqMsg = SignalProto.SendMsgReq.parseFrom(reqFull.getBody());
//                    sendMessage(uchnl, reqMsg);
                    eventProducer.post(new SendMsgEvent(uchnl, msg, reqHeader, reqMsg));
                    break;
                case SignalProto.Cmd.REPORT_USER_PROCESS_VALUE:
                    SignalProto.UserProcessSig userProcessSig = SignalProto.UserProcessSig.parseFrom(reqFull.getBody());
//                    this.reportProcess(uchnl, reqHeader, userProcessSig);
                    eventProducer.post(new ReportProcessEvent(uchnl, msg, reqHeader, userProcessSig));
                    break;
            }
        } catch (InvalidProtocolBufferException e) {
            log.error("pare pb body error:", e);
        }

    }



    //绑定用户到长连接通道
    private UserChannel bindUser(SecpChannel secpChnl, SignalProto.ClientRequestHeader header, SignalProto.BindUserSig body) {
        UserChannel.ID id = new UserChannel.ID(header.getUserId(), header.getClientType(), header.getCompanyId());
        UserChannel uchnl = new UserChannel(secpChnl, id, header.getToken());
        //解绑
        if (body != null && body.getOperation() == 0) {
            channelClosed(secpChnl);
            sendResponse(uchnl, SignalProto.Cmd.BIND_USER_RES_VALUE, IMError.SUCCESS, null);
            return null;
        }
        UserChannel oldChnl = secpChnl.getChannel().attr(USERCHANNEL).get();
        //是否重复绑定
        if (oldChnl != null
                && Objects.equals(oldChnl.getId(), id)
                && Objects.equals(oldChnl.getToken(), header.getToken())
                && oldChnl.getChannelId() == secpChnl.getId()) {
            log.info("repeat bindUser userId:{} companyId:{} token:{}", header.getUserId(), header.getCompanyId(), header.getToken());
            if (header.getCmd() == SignalProto.Cmd.BIND_USER_VALUE) {
                sendResponse(uchnl, SignalProto.Cmd.BIND_USER_RES_VALUE, IMError.SUCCESS, null);
                return oldChnl;
            }
        }

        log.info("receive bindUser userId:{} companyId:{} token:{}",header.getUserId(),header.getCompanyId(),header.getToken());
        //验证token
        boolean validToken = auth(header);
        if (!validToken) {
            sendResponse(uchnl, SignalProto.Cmd.BIND_USER_RES_VALUE, CommonError.ERROR_INVALID_TOKEN, null);
            uchnl.close("by invalid token");
            return null;
        }
        if (oldChnl != null && !oldChnl.getId().equals(uchnl.getId())) {
            userChnlMgrV2.remove(uchnl.getChannelId());
            log.info("channel override old:{} new:{} channelId:{}", oldChnl.getId(), uchnl.getId(), secpChnl.getId());
        }
        userChnlMgrV2.add(uchnl);

        secpChnl.getChannel().attr(USERCHANNEL).set(uchnl);

        log.info("bind userId:{} companyId:{} clientType:{} to channel:{}, localIP:{}", id.getUserId(), header.getCompanyId(), id.getClientType(), secpChnl.getId(), localIP);
        if (header.getCmd() == SignalProto.Cmd.BIND_USER_VALUE) {
            sendResponse(uchnl, SignalProto.Cmd.BIND_USER_RES_VALUE, IMError.SUCCESS, null);
        } else {
            SignalProto.SignalPush.Builder builder = SignalProto.SignalPush.newBuilder();
            SignalProto.ServerPushHeader pushh = SignalProto.ServerPushHeader.newBuilder()
                    .setVersion(1)
                    .setCmd(SignalProto.Cmd.BIND_USER_RES_VALUE)
                    .build();
            SignalProto.SignalPush pushData = builder.setHeader(pushh).build();
            try {
                uchnl.push(pushData);
            } catch (Exception e) {
                log.error("push data error:", e);
            }
        }

        //如果是新创建的的UserChannel, 发布一个用户上线的消息
        rabbitPublisher.sendByDirect(routingKey, RabbitPublisher.MQ_CMD_USERRLC_ONLINE, uchnl.getId());

        return uchnl;

    }

    private boolean auth(SignalProto.ClientRequestHeader header) {
        // 验证token参数
//        UserAuthorizedParam userAuthorizedParam = new UserAuthorizedParam();
//        userAuthorizedParam.setClientType(String.valueOf(header.getClientType()));
//        userAuthorizedParam.setToken(header.getToken());
//        userAuthorizedParam.setUserId(header.getUserId());
//        userAuthorizedParam.setCompanyId(header.getCompanyId());
//        //验证token有效性
//        return authService.authorizedUser(userAuthorizedParam);
        return true;
    }

    //长连接通道关闭
    public void channelClosed(SecpChannel secpChnl) {
        UserChannel uchnl = secpChnl.getChannel().attr(USERCHANNEL).get();
        if (null == uchnl) return;

        boolean removeSuccess = userChnlMgrV2.remove(uchnl.getChannelId()) != null;
//        boolean removeSuccess = userChnlMgr.removeEquals(uchnl.getId(), uchnl);
//        if (removeSuccess) {
//            //注销长连接通道
//            lcService.deleteLCUser(uchnl.getUserId(), String.valueOf(uchnl.getClientType()), uchnl.getId().getCompanyId());
//        }
        log.info("user channel closed. userId:{} companyId:{} clientType:{} channelId:{} removeSuccess:{}",
                uchnl.getUserId(), uchnl.getCompanyId(), uchnl.getClientType(), secpChnl.getId(), removeSuccess);
        eventProducer.post(new BackProcessEvent(uchnl.getUserId(), uchnl.getCompanyId(), uchnl.getClientType()));
    }

    //发送response
    private void sendResponse(UserChannel uchnl, int cmdId, IMError ie, MessageLite data) {
        sendResponse(uchnl, cmdId, ie.getCode(), ie.getMsg(), data);
    }

    private void sendResponse(UserChannel uchnl, int cmdId, CommonError ce, MessageLite data) {
        sendResponse(uchnl, cmdId, ce.getCode(), ce.getMsg(), data);
    }

    private void sendResponse(UserChannel uchnl, int cmdId, int errCode, String errMsg, MessageLite data) {
        SignalProto.ResponseHeader header = SignalProto.ResponseHeader.newBuilder()
                .setVersion(1)
                .setCmd(cmdId)
                .setCode(errCode)
                .setErrorMsg(errMsg)
                .build();

        SignalProto.SignalResponse.Builder builder = SignalProto.SignalResponse.newBuilder()
                .setHeader(header);
        if (null != data) {
            builder.setBody(data.toByteString());
        }
        SignalProto.SignalResponse res = builder.build();
        try {
            uchnl.response(secpRequest.get(), res);
        } catch (Exception e) {
            log.error("response error:", e);
        }

    }
}

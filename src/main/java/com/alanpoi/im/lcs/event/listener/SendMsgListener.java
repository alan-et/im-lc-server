package com.alanpoi.im.lcs.event.listener;

import com.alanpoi.im.lcs.imsignal.SignalException;
import com.alanpoi.im.lcs.imsignal.SignalProto;

import com.alanpoi.im.meeting.service.IMeetingService;
import com.alanpoi.im.meeting.service.MeetingRsp;
import com.alanpoi.im.meeting.service.vo.MeetingChatReq;
import com.alanpoi.im.meeting.service.vo.MeetingChatVO;
import com.alanpoi.im.message.service.MessageService;
import com.alanpoi.im.message.service.req.MsgSendReq;
import com.alanpoi.im.message.service.rsp.MessageException;
import com.alanpoi.im.message.service.rsp.MsgSendVO;
import com.qzd.im.common.event2.annotation.EventMapping;
import com.alanpoi.im.lcs.IMError;
import com.alanpoi.im.lcs.event.EventConfig;
import com.alanpoi.im.lcs.event.model.SendMsgEvent;
import com.alanpoi.im.lcs.imsignal.UserChannel;
import com.alanpoi.im.lcs.util.ResponseUtil;
import org.apache.dubbo.config.annotation.DubboReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class SendMsgListener {
    private static Logger logger = LoggerFactory.getLogger(SendMsgListener.class);
    @DubboReference
    private MessageService messageService;

    @DubboReference
    private IMeetingService meetingService;

    @EventMapping(executor = EventConfig.EXECUTOR_SEND_MSG)
    public void onSendMsg(SendMsgEvent event) {
        UserChannel userChannel = event.getUserChannel();
        SignalProto.SendMsgReq req = event.getReq();

        //转发到message模块
        logger.info("sendMessage conversation:{} from:{} to:{} contentType:{} content:{}",
                req.getConversation(), req.getFrom(), req.getTo(), req.getContentType(), trimContent(req.getContent()));
        //调用发送消息接口
        SignalProto.SendMsgRes res = null;
        int code = IMError.SUCCESS.getCode();
        String errMsg = IMError.SUCCESS.getMsg();
        try {
//            UserChannel.ID id = userChannel.getId();
            if (req.getConversation() == SignalProto.ConversationType.MEETING) {
                res = callSendMsgByMeet(userChannel, req);
            } else {
                res = callSendMsg(userChannel, req);
            }
//            }
            logger.info("sendMsg success convId:[{}] msgId:[{}]", req.getTo(), res.getMessageId());
        } catch (SignalException e) {
            code = e.getCode();
            errMsg = e.getMessage();
        }
        ResponseUtil.respond(event.getSecpMessage(), userChannel, SignalProto.Cmd.SEND_MSG_RES_VALUE, code, errMsg, res);
    }

    public SignalProto.SendMsgRes callSendMsg(UserChannel userChannel, SignalProto.SendMsgReq msg) throws SignalException {
        MsgSendVO msgSendVO = null;
        try {
            //构建请求参数结构
            MsgSendReq msgSendReq = new MsgSendReq();
            msgSendReq.setConversation(Byte.valueOf(msg.getConversation().getNumber() + ""));
            msgSendReq.setFrom(msg.getFrom());
            msgSendReq.setTo(msg.getTo());
            msgSendReq.setClientType(userChannel.getClientType());
            msgSendReq.setContentType(msg.getContentType());
            msgSendReq.setContent(msg.getContent());
            msgSendReq.setClientMsgId(msg.getClientMsgId());
            msgSendReq.setCustomInfo(msg.getCustomInfo());
            msgSendReq.setIdempotentId(msg.getIdempotentId());
            //RPC调用消息接口
            msgSendVO = messageService.sendMessage(msgSendReq);

            SignalProto.SendMsgRes res = SignalProto.SendMsgRes.newBuilder()
                    .setConversation(msg.getConversation())
                    .setMessageId(msgSendVO.getMessageId())
                    .build();

            return res;
        } catch (MessageException e) {
            logger.error("callSendMsg error convId:[{}]", msg.getTo(), e);
            throw new SignalException(e.getCode(), e.getMessage());
        } catch (Exception e1) {
            logger.error("callSendMsg error convId:[{}]", msg.getTo(), e1);
            throw new SignalException(IMError.UNKNOWN);
        }
    }


    public SignalProto.SendMsgRes callSendMsgByMeet(UserChannel userChannel, SignalProto.SendMsgReq msg) throws SignalException {
        if (msg == null || StringUtils.isEmpty(msg.getTo()) || StringUtils.isEmpty(msg.getContent())) {
            throw new SignalException(IMError.SEND_MSG_FAIL.getCode(), "会议号和消息内容不能为空");
        }
        try {
            MeetingChatReq meetingChatReq = new MeetingChatReq();
            meetingChatReq.setMeetingId(msg.getTo());
            meetingChatReq.setContentType(msg.getContentType());
            meetingChatReq.setContent(msg.getContent());
            meetingChatReq.setClientMsgId(msg.getClientMsgId());
            meetingChatReq.setCustomInfo(msg.getCustomInfo());
            if (msg.hasFromName()) {
                meetingChatReq.setFromUsername(msg.getFromName());
            }
            String userId = userChannel != null && !StringUtils.isEmpty(userChannel.getUserId())
                    ? userChannel.getUserId() : msg.getFrom();
            MeetingRsp<MeetingChatVO> rpcRes = meetingService.broadcastChat(userId, meetingChatReq);
            if (rpcRes == null) {
                throw new SignalException(IMError.UNKNOWN);
            }
            if (!rpcRes.ok()) {
                logger.error("callSendMsgByMeet fail userId:[{}] convId:[{}] code:[{}] msg:[{}]",
                        userId, msg.getTo(), rpcRes.getErrorCode(), rpcRes.getErrorMsg());
                throw new SignalException(rpcRes.getErrorCode(), rpcRes.getErrorMsg());
            }
            MeetingChatVO vo = rpcRes.getData();
            String messageId = vo != null && !StringUtils.isEmpty(vo.getMessageId())
                    ? vo.getMessageId() : msg.getClientMsgId();
            return SignalProto.SendMsgRes.newBuilder()
                    .setConversation(SignalProto.ConversationType.MEETING)
                    .setMessageId(messageId == null ? "" : messageId)
                    .build();
        } catch (SignalException e) {
            throw e;
        } catch (Exception e1) {
            logger.error("callSendMsgByMeet error userId:[{}] convId:[{}]", msg.getFrom(), msg.getTo(), e1);
            throw new SignalException(IMError.UNKNOWN);
        }
    }


    public static String trimContent(String content) {
        if (StringUtils.isEmpty(content)) return content;
        content = content.trim();

        int retainSize = 3;
        if (content.length() <= retainSize * 2) return content;
        String prefix = content.substring(0, retainSize);
        String suffix = content.substring(content.length() - retainSize);
        return prefix + "****" + suffix + "(" + content.length() + ")";
    }
}

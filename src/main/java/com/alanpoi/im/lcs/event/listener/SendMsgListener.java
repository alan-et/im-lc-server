package com.alanpoi.im.lcs.event.listener;

import com.alanpoi.im.lcs.imsignal.SignalException;
import com.alanpoi.im.lcs.imsignal.SignalProto;
//import com.alibaba.csp.sentinel.Entry;
//import com.alibaba.csp.sentinel.EntryType;
//import com.alibaba.csp.sentinel.SphU;
//import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alanpoi.im.message.service.MessageService;
import com.alanpoi.im.message.service.req.MsgSendReq;
import com.alanpoi.im.message.service.rsp.MsgSendVO;
import com.alibaba.dubbo.config.annotation.Reference;
import com.qzd.im.common.event2.annotation.EventMapping;
import com.qzd.im.common.response.CommonError;
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
            UserChannel.ID id = userChannel.getId();
//            try (Entry entry = SphU.entry("SendMsg", EntryType.IN, 1,
//                    id.getUserId(), id.getCompanyId(), req.getTo())) {
                res = callSendMsg(userChannel, req);
//            }
            logger.info("sendMsg success convId:[{}] msgId:[{}]", req.getTo(), res.getMessageId());
        }
        catch (SignalException e) {
            code = e.getCode();
            errMsg = e.getMessage();
        }
//        catch (BlockException e){
//            code = CommonError.ERROR_REQUEST_FREQUENTLY.getCode();
//            errMsg = CommonError.ERROR_REQUEST_FREQUENTLY.getMsg();
//        }
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
            msgSendReq.setContentType(msg.getContentType());
            msgSendReq.setContent(msg.getContent());
            msgSendReq.setClientMsgId(msg.getClientMsgId());
            msgSendReq.setCustomInfo(msg.getCustomInfo());
            msgSendReq.setIdempotentId(msg.getIdempotentId());
            msgSendReq.setCompanyId(userChannel.getId().getCompanyId());
            //RPC调用消息接口
            msgSendVO = messageService.sendMessage(msgSendReq);

            SignalProto.SendMsgRes res = SignalProto.SendMsgRes.newBuilder()
                    .setConversation(msg.getConversation())
                    .setMessageId(msgSendVO.getMessageId())
                    .build();

            return res;
        } catch (Exception e) {
            logger.error("callSendMsg error convId:[{}]", msg.getTo(), e);
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

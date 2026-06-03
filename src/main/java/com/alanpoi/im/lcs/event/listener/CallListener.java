package com.alanpoi.im.lcs.event.listener;

import com.alanpoi.im.lcs.IMError;
import com.alanpoi.im.lcs.event.EventConfig;
import com.alanpoi.im.lcs.event.model.CallEvent;
import com.alanpoi.im.lcs.imsignal.SignalProto;
import com.alanpoi.im.lcs.imsignal.UserChannel;
import com.alanpoi.im.lcs.util.ResponseUtil;
import com.alanpoi.im.message.service.MessageService;
import com.qzd.im.common.event2.annotation.EventMapping;
import org.apache.dubbo.config.annotation.DubboReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CallListener {
    private static final Logger logger = LoggerFactory.getLogger(CallListener.class);

    @DubboReference
    private MessageService messageService;

    @EventMapping(executor = EventConfig.EXECUTOR_CALL)
    public void onCall(CallEvent event) {
        UserChannel userChannel = event.getUserChannel();
        SignalProto.OthBusiSig req = event.getReq();

        int code = IMError.SUCCESS.getCode();
        String errMsg = IMError.SUCCESS.getMsg();
        try {
            logger.info("otherBusi code:{} toUserId in data", req.getCode());
            boolean ok = messageService.call(req.getCode(), req.getData());
            if (!ok) {
                code = IMError.UNKNOWN.getCode();
                errMsg = "call forward failed";
            }
        } catch (Exception e) {
            logger.error("call forward error", e);
            code = IMError.UNKNOWN.getCode();
            errMsg = e.getMessage() != null ? e.getMessage() : IMError.UNKNOWN.getMsg();
        }
        ResponseUtil.respond(
                event.getSecpMessage(),
                userChannel,
                SignalProto.Cmd.OTHER_BUSI_VALUE,
                code,
                errMsg,
                null
        );
    }
}

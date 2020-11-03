package com.alanpoi.im.lcs.util;

import com.google.protobuf.MessageLite;
import com.qzd.im.common.response.CommonError;
import com.alanpoi.im.lcs.IMError;
import com.alanpoi.im.lcs.imsignal.SignalProto;
import com.alanpoi.im.lcs.imsignal.UserChannel;
import com.alanpoi.im.lcs.secprotocol.SecpMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResponseUtil {
    private static Logger logger = LoggerFactory.getLogger(ResponseUtil.class);

    //发送response
    public static void respond(SecpMessage req, UserChannel uchnl, int cmdId, IMError ie, MessageLite data) {
        respond(req, uchnl, cmdId, ie.getCode(), ie.getMsg(), data);
    }

    public static void respond(SecpMessage req, UserChannel uchnl, int cmdId, CommonError ce, MessageLite data) {
        respond(req, uchnl, cmdId, ce.getCode(), ce.getMsg(), data);
    }

    public static void respond(SecpMessage req, UserChannel uchnl, int cmdId, int errCode, String errMsg, MessageLite data) {
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
            uchnl.response(req, res);
        } catch (Exception e) {
            logger.error("response error:", e);
        }

    }

}

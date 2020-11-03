package com.alanpoi.im.lcs.event.listener;

import com.alibaba.dubbo.config.annotation.Reference;
import com.qzd.im.common.constants.TimeConstants;
import com.qzd.im.common.event2.annotation.EventMapping;
import com.qzd.im.common.id.ServerID;
import com.alanpoi.im.lcs.IMError;
import com.alanpoi.im.lcs.event.model.BackProcessEvent;
import com.alanpoi.im.lcs.event.model.ReportProcessEvent;
import com.alanpoi.im.lcs.imsignal.SignalProto;
import com.alanpoi.im.lcs.util.ResponseUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class DefaultListener {
    private static Logger logger = LoggerFactory.getLogger(DefaultListener.class);


//    @Reference
//    private PushSvc pushSvc;

    @Autowired
    private ServerID serverID;
    @Autowired
    private StringRedisTemplate redis;

    /**
     * 用户主动上报前后台进程
     */
    @EventMapping
    public void onReportProcess(ReportProcessEvent event) {
        SignalProto.ClientRequestHeader header = event.getReqHeader();
        SignalProto.UserProcessSig userProcessSig = event.getReq();

        int clientType = header.getClientType();
        int processState = userProcessSig.getProcessState();
        String userId = header.getUserId();
        String companyId = header.getCompanyId();
        logger.info("reportProcess userId:{} companyId:[{}], clientClient:{}, state:{}", userId, companyId, clientType, processState);

        int code = IMError.SUCCESS.getCode();
        String errMsg = IMError.SUCCESS.getMsg();
        //移动端上报才写入
        if (processState == 1) {
            //前台
            frontProcess(userId, companyId, clientType);
        } else if (processState == 0) {
            //后台
            Integer badge = null;
            if (userProcessSig.hasBadge()) {
                badge = userProcessSig.getBadge();
            }
            backProcess(userId, companyId, clientType, badge);
        }
        ResponseUtil.respond(event.getSecpMessage(), event.getUserChannel(), SignalProto.Cmd.REPORT_USER_PROCESS_RES_VALUE, code, errMsg, null);
    }

    /**
     * 用户长链接断开时触发
     */
    @EventMapping
    public void onBackProcess(BackProcessEvent event) {
        backProcess(event.getUserId(), event.getCompanyId(), event.getClientType(), null);
    }

    private void frontProcess(String userId, String companyId, int clientType) {
        logger.info("frontProcess userId:[{}] companyId:[{}] clientType:[{}] ",
                userId, companyId, clientType);
        //移动端上报才写入
        if (clientType == SignalProto.ClientType.IPHONE_VALUE
                || clientType == SignalProto.ClientType.IPAD_VALUE
                || clientType == SignalProto.ClientType.ANDROID_VALUE) {
            String key = "UserProcess:" + userId + ":" + companyId;
            String servId = "" + serverID.getId();
            redis.opsForValue().set(key, servId, TimeConstants.DAY_15, TimeUnit.SECONDS);
        }
    }

    private void backProcess(String userId, String companyId, int clientType, Integer badge) {
        logger.info("backProcess userId:[{}] companyId:[{}] clientType:[{}] badge:[{}]",
                userId, companyId, clientType, badge);
        //移动端上报才写入
        if (clientType == SignalProto.ClientType.IPHONE_VALUE
                || clientType == SignalProto.ClientType.IPAD_VALUE
                || clientType == SignalProto.ClientType.ANDROID_VALUE) {
            String key = "UserProcess:" + userId + ":" + companyId;

            String servIdInRedis = redis.opsForValue().get(key);
            String servId = "" + serverID.getId();
            if (StringUtils.equals(servIdInRedis, servId)) {
                redis.delete(key);
            }
            //后台
            if (badge != null) {
//                pushSvc.setBadge(userId, companyId, badge);
            }
        }
    }


}

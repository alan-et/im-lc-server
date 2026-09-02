package com.alanpoi.im.lcs.event.listener;

import com.alanpoi.im.lcs.IMError;
import com.alanpoi.im.lcs.event.EventConfig;
import com.alanpoi.im.lcs.event.model.CallSignalEvent;
import com.alanpoi.im.lcs.imsignal.SignalProto;
import com.alanpoi.im.lcs.imsignal.UserChannel;
import com.alanpoi.im.lcs.util.ResponseUtil;
import com.alanpoi.im.sigTranspond.services.ISigMessageService;
import com.alanpoi.im.sigTranspond.services.req.CallSignalReq;
import com.alanpoi.im.sigTranspond.services.rsp.Response;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alanpoi.im.common.event2.annotation.EventMapping;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 客户端上行 CALL_SIGNAL：校验后转发。
 * - RINGING / ACCEPT:纯回执/状态推送,直接转调信令服务 fan-out(目标用户可能在其他 LC 节点,由 sigTranspond 按用户路由)。
 * - REJECT / CANCEL / BUSY / HANGUP:通话终态,统一改走 im-api 对应 REST 接口——那边除了 fan-out,
 *   还会顺带生成通话记录消息(contentType=800),避免这块逻辑在 im-lc-server / im-api 两处各写一份。
 */
@Component
public class CallSignalListener {
    private static final Logger logger = LoggerFactory.getLogger(CallSignalListener.class);

    @DubboReference
    private ISigMessageService sigMessageService;

    @Value("${im.api.baseUrl:http://127.0.0.1:8080}")
    private String imApiBaseUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @EventMapping(executor = EventConfig.EXECUTOR_CALL)
    public void onCallSignal(CallSignalEvent event) {
        UserChannel sender = event.getUserChannel();
        SignalProto.CallSignal req = event.getReq();
        String senderUserId = event.getReqHeader().getUserId();

        int code = IMError.SUCCESS.getCode();
        String errMsg = IMError.SUCCESS.getMsg();

        try {
            if (req == null || StringUtils.isEmpty(req.getCallId())) {
                code = IMError.UNKNOWN.getCode();
                errMsg = "callId 不能为空";
            } else if (req.getType() == SignalProto.CallSignalType.CALL_INVITE_VALUE) {
                code = IMError.UNKNOWN.getCode();
                errMsg = "客户端不可发送 CALL_INVITE";
            } else if (!isClientAllowedType(req.getType())) {
                code = IMError.UNKNOWN.getCode();
                errMsg = "不支持的通话信令类型: " + req.getType();
            } else if (StringUtils.isNotEmpty(req.getFromUserId())
                    && !senderUserId.equals(req.getFromUserId())) {
                code = IMError.UNKNOWN.getCode();
                errMsg = "fromUserId 与当前用户不一致";
            } else {
                List<String> toUserIds = collectTargets(req, senderUserId);
                if (toUserIds.isEmpty()) {
                    code = IMError.UNKNOWN.getCode();
                    errMsg = "toUserIds 不能为空";
                } else if (isTerminalType(req.getType())) {
                    logger.info("callSignal uplink(->im-api) type={} callId={} from={} to={}",
                            req.getType(), req.getCallId(), senderUserId, toUserIds);
                    proxyToApi(req, senderUserId, toUserIds, event);
                } else {
                    CallSignalReq sigReq = toCallSignalReq(req, senderUserId, toUserIds, event);
                    logger.info("callSignal uplink type={} callId={} from={} to={}",
                            sigReq.getType(), sigReq.getCallId(), senderUserId, toUserIds);
                    Response resp = sigMessageService.callSignalNotice(sigReq);
                    if (resp == null || resp.getCode() == null || resp.getCode() != 0) {
                        code = IMError.UNKNOWN.getCode();
                        errMsg = resp != null && resp.getErrorMsg() != null
                                ? resp.getErrorMsg() : "callSignal forward failed";
                    }
                }
            }
        } catch (Exception e) {
            logger.error("callSignal forward error", e);
            code = IMError.UNKNOWN.getCode();
            errMsg = e.getMessage() != null ? e.getMessage() : IMError.UNKNOWN.getMsg();
        }

        ResponseUtil.respond(
                event.getSecpMessage(),
                sender,
                SignalProto.Cmd.CALL_SIGNAL_VALUE,
                code,
                errMsg,
                null
        );
    }

    private boolean isTerminalType(int type) {
        switch (type) {
            case SignalProto.CallSignalType.CALL_REJECT_VALUE:
            case SignalProto.CallSignalType.CALL_CANCEL_VALUE:
            case SignalProto.CallSignalType.CALL_BUSY_VALUE:
            case SignalProto.CallSignalType.CALL_HANGUP_VALUE:
                return true;
            default:
                return false;
        }
    }

    /**
     * 拒绝/取消/忙线/挂断:转调 im-api 对应 REST 接口,而不是直接调 sigMessageService。
     * im-api 里那几个接口本身就会做 fan-out + 生成通话记录消息,这里只是把 WS 上行的信令原样转成一次内部 HTTP 调用。
     * 鉴权头(userId/clientType/apptoken)直接用当前 WS 连接已认证的身份透传,不代表真实客户端在发起 REST 调用。
     */
    private void proxyToApi(SignalProto.CallSignal req, String senderUserId, List<String> toUserIds, CallSignalEvent event) {
        String path;
        JSONObject body = new JSONObject(true);
        body.put("roomName", req.getRoomName());
        body.put("toUserIds", toUserIds);
        body.put("fromUsername", req.getFromUsername());
        body.put("fromUserIcon", req.getFromUserIcon());
        if (StringUtils.isNotEmpty(req.getGroupId())) {
            body.put("groupId", req.getGroupId());
        }
        if (StringUtils.isNotEmpty(req.getMediaType())) {
            body.put("mediaType", req.getMediaType());
        }
        switch (req.getType()) {
            case SignalProto.CallSignalType.CALL_REJECT_VALUE:
                path = "/im/call/reject";
                break;
            case SignalProto.CallSignalType.CALL_CANCEL_VALUE:
                path = "/im/call/cancel";
                break;
            case SignalProto.CallSignalType.CALL_BUSY_VALUE:
                path = "/im/call/busy";
                break;
            case SignalProto.CallSignalType.CALL_HANGUP_VALUE:
                path = "/im/call/hangup";
                body.put("duration", extractDuration(req.getPayload()));
                break;
            default:
                throw new IllegalArgumentException("not a terminal call signal type: " + req.getType());
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("userId", senderUserId);
        headers.set("clientType", String.valueOf(event.getReqHeader().getClientType()));
        String token = event.getReqHeader().getToken();
        // 鉴权头已由 apptoken 改名为 token;两个都发,兼容尚未升级的 im-api 实例
        String tokenValue = StringUtils.isNotEmpty(token) ? token : "internal-ws-relay";
        headers.set("token", tokenValue);
        headers.set("apptoken", tokenValue);
        if (StringUtils.isNotEmpty(event.getReqHeader().getCompanyId())) {
            headers.set("companyId", event.getReqHeader().getCompanyId());
        }
        restTemplate.postForEntity(imApiBaseUrl + path, new HttpEntity<>(body.toJSONString(), headers), String.class);
    }

    /** CALL_HANGUP 的 payload 约定为 {"duration": <秒>}(客户端本地算好的通话时长) */
    private int extractDuration(String payload) {
        if (StringUtils.isEmpty(payload)) {
            return 0;
        }
        try {
            JSONObject json = JSON.parseObject(payload);
            Integer duration = json.getInteger("duration");
            return duration == null ? 0 : Math.max(0, duration);
        } catch (Exception e) {
            logger.warn("extractDuration parse failed, payload:[{}]", payload, e);
            return 0;
        }
    }

    private boolean isClientAllowedType(int type) {
        switch (type) {
            case SignalProto.CallSignalType.CALL_RINGING_VALUE:
            case SignalProto.CallSignalType.CALL_ACCEPT_VALUE:
            case SignalProto.CallSignalType.CALL_REJECT_VALUE:
            case SignalProto.CallSignalType.CALL_CANCEL_VALUE:
            case SignalProto.CallSignalType.CALL_BUSY_VALUE:
            case SignalProto.CallSignalType.CALL_HANGUP_VALUE:
            // 振铃超时:主叫直发 CALL_END 让被叫停止响铃,随后自己退房。
            // 刻意不放进 isTerminalType —— 它只做纯 fan-out 转发,不转去 im-api 写通话记录;
            // 记录由主叫退房后 LiveKit webhook 触发的 finalizeRoom 生成("无人应答")。
            case SignalProto.CallSignalType.CALL_END_VALUE:
                return true;
            default:
                return false;
        }
    }

    private List<String> collectTargets(SignalProto.CallSignal sig, String senderUserId) {
        Set<String> targetIds = new LinkedHashSet<>();
        if (StringUtils.isNotEmpty(sig.getToUserId())) {
            targetIds.add(sig.getToUserId());
        }
        for (String uid : sig.getToUserIdsList()) {
            if (StringUtils.isNotEmpty(uid)) {
                targetIds.add(uid);
            }
        }
        targetIds.remove(senderUserId);
        return new ArrayList<>(targetIds);
    }

    private CallSignalReq toCallSignalReq(SignalProto.CallSignal sig, String senderUserId,
                                          List<String> toUserIds, CallSignalEvent event) {
        CallSignalReq req = new CallSignalReq();
        req.setCallId(sig.getCallId());
        req.setType(sig.getType());
        req.setFromUserId(StringUtils.isNotEmpty(sig.getFromUserId()) ? sig.getFromUserId() : senderUserId);
        if (StringUtils.isNotEmpty(sig.getFromUsername())) {
            req.setFromUsername(sig.getFromUsername());
        }
        if (StringUtils.isNotEmpty(sig.getFromUserIcon())) {
            req.setFromUserIcon(sig.getFromUserIcon());
        }
        req.setToUserIds(toUserIds);
        if (StringUtils.isNotEmpty(sig.getGroupId())) {
            req.setGroupId(sig.getGroupId());
        }
        if (StringUtils.isNotEmpty(sig.getMediaType())) {
            req.setMediaType(sig.getMediaType());
        }
        if (StringUtils.isNotEmpty(sig.getMode())) {
            req.setMode(sig.getMode());
        }
        if (StringUtils.isNotEmpty(sig.getRoomName())) {
            req.setRoomName(sig.getRoomName());
        }
        if (StringUtils.isNotEmpty(sig.getPayload())) {
            req.setPayload(sig.getPayload());
        }
        if (sig.getMembersCount() > 0) {
            req.setMembers(sig.getMembersList());
        }
        req.setTimestamp(sig.getTimestamp() > 0 ? sig.getTimestamp() : System.currentTimeMillis());
        req.setCompanyId(event.getReqHeader().getCompanyId());
        return req;
    }
}

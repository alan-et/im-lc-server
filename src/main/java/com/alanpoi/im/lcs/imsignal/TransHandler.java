package com.alanpoi.im.lcs.imsignal;

import com.alanpoi.im.lcs.transtools.network.Frame;
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.ProtocolStringList;
import com.googlecode.protobuf.format.JsonFormat;
import io.netty.channel.Channel;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 *处理来自sigTranspond的信令转发请求
 */

@Component
public class TransHandler {
    private static Logger logger = LoggerFactory.getLogger(TransHandler.class);

    @Autowired
    private UserChannelManager userChnlMgr;

    private JsonFormat format = new JsonFormat();


    public void push(Channel chnl, Frame req) {
        try {
            SignalProto.SignalTranspondRes.Builder sigResBuilder = SignalProto.SignalTranspondRes
                    .newBuilder();

            Frame res = new Frame();
            res.setCmd(Frame.CMD_DATA_RES);
            res.setSeqId(req.getSeqId());
            res.setBody(sigResBuilder.build().toByteArray());

            byte[] body = req.getBody();
            if (null == body) {
                chnl.writeAndFlush(res);
                return;
            }

            SignalProto.SignalTranspond sigts = parseBody(body);
            if (sigts == null) {
                chnl.writeAndFlush(res);
                return;
            }

            List<UserChannel> channelList = findChannels(sigts);
            SignalProto.SignalTranspondRes sigRes = pushData(channelList, sigts);
            if (null != sigRes) {
                res.setBody(sigRes.toByteArray());
            }
            chnl.writeAndFlush(res);
        }catch (Exception e){
            logger.error("", e);
        }

    }

    private SignalProto.SignalTranspondRes pushData(List<UserChannel> channelList, SignalProto.SignalTranspond sigts) {
        if (channelList == null || channelList.size() == 0) return null;
        boolean closeChannel = isCloseChannel(sigts);
        int cmd = sigts.getHeader().getCmd();
        SignalProto.ServerPushHeader phd = SignalProto.ServerPushHeader
                .newBuilder()
                .setVersion(1)
                .setCmd(cmd)
                .build();
        SignalProto.SignalPush pdata = SignalProto.SignalPush
                .newBuilder()
                .setHeader(phd)
                .setBody(sigts.getBody())
                .build();

        SignalProto.SignalTranspondRes.Builder sigResBuilder = SignalProto.SignalTranspondRes
                .newBuilder();

        for (UserChannel userChannel : channelList) {
            UserChannel.ID id = userChannel.getId();
            try {
                userChannel.push(pdata);

                SignalProto.SignalTranspondRes.UserClient uc =
                        SignalProto.SignalTranspondRes.UserClient.newBuilder()
                                .setUserId(id.getUserId())
                                .setCompanyId(id.getCompanyId())
                                .setClientType(id.getClientType())
                                .build();
                sigResBuilder.addPushSuccesses(uc);

            } catch (Exception e) {
                logger.error("pushData error,complexId:[{}],cmd:[{}],errormsg:{}",
                        id, Integer.toHexString(cmd), e);
            }
            if (closeChannel) userChannel.close("by remote cmd " + cmd);
        }

        return sigResBuilder.build();
    }

    private boolean isCloseChannel(SignalProto.SignalTranspond sigts) {
        return sigts.getHeader().getCmd() == SignalProto.Cmd.OFFLINE_VALUE;
    }

    private List<UserChannel> findChannels(SignalProto.SignalTranspond sigts) {
        SignalProto.SignalTranspond.Header header = sigts.getHeader();
        String bodyStr = bodyToStr(sigts);
        List<UserChannel> result = new ArrayList<>();
        for (SignalProto.SignalTranspond.Target target : header.getTargetsList()) {
            String userId = target.getUserId();
            String companyId = target.getCompanyId();
            List<Integer> clientTypes = target.getClientTypeList();

            List<UserChannel> channelList = null;
            if (StringUtils.isEmpty(companyId)) {
                channelList = userChnlMgr.findByUserId(userId, clientTypes);
            } else {
                channelList = userChnlMgr.findByPersonId(userId, companyId, clientTypes);
            }
            //todo amend user servinfo
            if (channelList != null && channelList.size() > 0) {

                //白名单  包含在里边的才进行推送
                ProtocolStringList whiteList = target.getWhiteTokenList();
                if (whiteList != null && whiteList.size() > 0) {
                    channelList = channelList.stream()
                            .filter(uc -> whiteList.contains(uc.getToken()))
                            .collect(Collectors.toList());
                }
                //黑名单  不在里边的才进行推送
                ProtocolStringList blackList = target.getBlackTokenList();
                if (blackList != null && blackList.size() > 0) {
                    channelList = channelList.stream()
                            .filter(uc -> !blackList.contains(uc.getToken()))
                            .collect(Collectors.toList());
                }

                result.addAll(channelList);
            }
            logger.info("push signal cmd:[{}] to user:[{}]-[{}], clientTypes--{}. channel count:{} body:[{}]",
                    String.format("0x%04X", header.getCmd()), userId, companyId, clientTypes,
                    channelList == null ? null : channelList.size(), bodyStr);
        }
        return result;
    }

    private String bodyToStr(SignalProto.SignalTranspond sigt) {
        StringBuilder sb = new StringBuilder();
        try {
            Any any = Any.parseFrom(sigt.getBody());
            format.print(any, sb);
        } catch (IOException e) {
            sb.append(sigt.getHeader().toString());
        }
        return sb.toString();
    }

    private SignalProto.SignalTranspond parseBody(byte[] body) {
        try {
            if(null == body){
                logger.error("body is null");
                return null;
            }
            return SignalProto.SignalTranspond.parseFrom(body);
        } catch (InvalidProtocolBufferException e) {
            logger.error("parse pb error:", e);
        }
        return null;
    }
}

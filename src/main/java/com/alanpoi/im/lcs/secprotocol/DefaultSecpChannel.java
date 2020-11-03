package com.alanpoi.im.lcs.secprotocol;

import com.alanpoi.im.lcs.secprotocol.channel.AbstractSecpChannel;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

public class DefaultSecpChannel extends AbstractSecpChannel {
    private static final Logger log = LoggerFactory.getLogger(DefaultSecpChannel.class);

    private long lcId = 0; //长连接会话id
    private String secKey; //长连接会话临时秘钥

    public DefaultSecpChannel(Channel chnl){
        super(chnl);
    }

    public void setLcId(long lcId){
        this.lcId = lcId;
    }
    public long getLcId(){
        return lcId;
    }

    public void setSecKey(String seckey){
        this.secKey = seckey;
    }
    public String getSecKey(){
        return secKey;
    }

    @Override
    public void write(SecpMessage msg){
        msg.setLcId(lcId);

        if(!encode(msg)) return;

        super.write(msg);
    }

    public void respondException(short code, SecpMessage req){
        SecpMessage res = null;
        if(null == req | (null != req && !req.needResponse())){
            res = new SecpMessage();
            res.setCmd(Cmd.TOC_EXCEPTION_REQ);
        }else{
            res = req;
        }
        res.setCode(code);

        write(res);
    }

    //消息编码
    private boolean encode(SecpMessage msg){
        byte[] body = msg.getBody();
        if(null == body || body.length == 0){
            return true;
        }

        String secKey = getSecKey();
        if(StringUtils.isEmpty(secKey)){
            log.error("can't find seckey. chnlId:{} lcId:{}", getId(), getLcId());
            respondException(Errors.LCID_INVALID, null);
            close("can't find seckey");
            return false;
        }

        //压缩
        SecpMsgEncoder.compress(msg);
        //加密
        if(!SecpMsgEncoder.encrypt(secKey, msg)){
            log.info("encrypt message error. chnlId:{} lcId:{}", getId(), getLcId());
            respondException(Errors.UNKNOWN, null);
            close("encrypt message error");
            return false;
        }

        return true;
    }

    //解码消息
    public boolean decode(SecpMessage msg){
        byte[] body = msg.getBody();
        if(null == body || body.length == 0){
            return true;
        }

        String secKey = getSecKey();
        if(StringUtils.isEmpty(secKey)){
            log.error("can't find seckey. chnlId:{} lcId:{}", getId(), getLcId());
            respondException(Errors.LCID_INVALID, msg);
            close("cant'find secKey.");
            return false;
        }
        //解密
        if(!SecpMsgDecoder.decrypt(secKey, msg)){
            log.error("decrypt error. chnlId:{} lcId:{}", getId(), getLcId());
            respondException(Errors.LCID_INVALID, msg);
            close("decrypt error.");
            return false;
        }
        //解压
        if(!SecpMsgDecoder.uncompress(msg)){
            log.error("uncompress error. chnlId:{} lcId:{}", getId(), getLcId());
            respondException(Errors.UNKNOWN, msg);
            close("uncompress error");
            return false;
        }

        return true;
    }


}

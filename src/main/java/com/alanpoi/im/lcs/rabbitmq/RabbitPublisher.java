package com.alanpoi.im.lcs.rabbitmq;

import com.alibaba.fastjson.JSON;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class RabbitPublisher {
    public static final String MQ_CMD_USERRLC_ONLINE = "userLCOnline";  //用户长连接上线

    @Autowired
    @Qualifier("directTemplate")
    private RabbitTemplate  directRabbit;

    /**
     * 使用使用direct类型的exchange发送消息
     * @param obj 消息内容
     */
    public void sendByDirect(String routingKey, String cmd, Object obj){
        MessageProperties properties =  new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        properties.setHeader("cmd", cmd);

        Message msg = MessageBuilder.withBody(JSON.toJSONBytes(obj))
                .andProperties(properties)
                .build()
                ;

        directRabbit.send(routingKey, msg);

    }
}

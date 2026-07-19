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

    /**
     * 消息投递用的direct交换机。
     *
     * 说明:此前这里发往默认交换机(RabbitTemplate 未 setExchange,exchange 为 ""),
     * 而默认交换机只会投递给"队列名恰好等于 routingKey"的那一个队列,
     * 导致 RabbitConf 中已声明的 amq.direct 绑定形同虚设、其它服务无法订阅同一事件。
     * 改为显式发往 amq.direct 后,凡是以相同 routingKey 绑定到该交换机的队列都能各收到一份,
     * 原有队列(名称与 routingKey 同名且已绑定 amq.direct)投递不受影响。
     */
    public static final String DIRECT_EXCHANGE = "amq.direct";

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

        directRabbit.send(DIRECT_EXCHANGE, routingKey, msg);

    }
}

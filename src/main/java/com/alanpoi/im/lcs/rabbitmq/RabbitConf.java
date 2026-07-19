package com.alanpoi.im.lcs.rabbitmq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;

@Configuration
public class RabbitConf {
    private static final Logger log = LoggerFactory.getLogger(RabbitConf.class);

    @Value("${qzdim.rabbitmq.virtual-host:}")
    private String vhost;

    @Value("${qzdim.rabbitmq.host:}")
    private String host;

    @Value("${qzdim.rabbitmq.port:}")
    private int port;

    @Value("${qzdim.rabbitmq.username:}")
    private String user;

    @Value("${qzdim.rabbitmq.password:}")
    private String password;

    @Value("${qzdim.rabbitmq.lcs.queue.direct.internal:}")
    private String queueDirect;

    @Bean
    public ConnectionFactory getConnectionFactory(){
        CachingConnectionFactory factory = new CachingConnectionFactory(host, port);
        factory.setVirtualHost(vhost);
        factory.setUsername(user);
        factory.setPassword(password);

        return factory;
    }

    @Bean
    public AmqpAdmin getAdmin(ConnectionFactory factory){
        AmqpAdmin admin =   new RabbitAdmin(factory);

        Queue queue = new Queue(queueDirect);
        admin.declareQueue(queue);

        Binding binding = new Binding(queue.getName(), Binding.DestinationType.QUEUE, "amq.direct", queue.getName(), new HashMap<String, Object>());
        admin.declareBinding(binding);

        return admin;
    }

    @Bean(name = "directTemplate")
    public RabbitTemplate getRabbitTemplate(ConnectionFactory factory){
        RabbitTemplate rabbit = new RabbitTemplate(factory);
        //rabbit.setRoutingKey(queueName);

        // mandatory + 返回回调:消息若没有匹配到任何队列(例如 amq.direct 上的绑定意外缺失),
        // broker 会把消息退回,这里打 ERROR 日志。否则这类投递会被 broker 静默丢弃、无任何痕迹,
        // 事后极难排查(用户上线事件丢失 → 下游订阅方全部失效)。
        rabbit.setMandatory(true);
        rabbit.setReturnsCallback(returned ->
                log.error("MQ message unroutable, dropped! exchange:[{}] routingKey:[{}] replyCode:[{}] replyText:[{}] body:[{}]",
                        returned.getExchange(),
                        returned.getRoutingKey(),
                        returned.getReplyCode(),
                        returned.getReplyText(),
                        new String(returned.getMessage().getBody())));
        return  rabbit;
    }


}

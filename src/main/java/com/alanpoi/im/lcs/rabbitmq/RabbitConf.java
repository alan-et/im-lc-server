package com.alanpoi.im.lcs.rabbitmq;

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
    @Value("${qzdim.rabbitmq.vhost:}")
    private String vhost;

    @Value("${qzdim.rabbitmq.host:}")
    private String host;

    @Value("${qzdim.rabbitmq.port:}")
    private int port;

    @Value("${qzdim.rabbitmq.user:}")
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
        return  rabbit;
    }


}

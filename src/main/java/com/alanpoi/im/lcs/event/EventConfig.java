package com.alanpoi.im.lcs.event;

import com.qzd.im.common.event2.DefaultExecutorManager;
import com.qzd.im.common.event2.EventProducer;
import com.qzd.im.common.event2.EventSpringSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EventConfig {
    public static final String EXECUTOR_SEND_MSG = "sendMsg";

    @Bean
    public EventProducer eventProducer() {
        DefaultExecutorManager container = new DefaultExecutorManager();
        container.create(EXECUTOR_SEND_MSG, 4, 16);
        return new EventProducer(container);
    }

    @Bean
    public EventSpringSupport support(@Autowired EventProducer eventProducer) {
        return new EventSpringSupport("com.alanpoi.im.lcs", eventProducer);
    }
}

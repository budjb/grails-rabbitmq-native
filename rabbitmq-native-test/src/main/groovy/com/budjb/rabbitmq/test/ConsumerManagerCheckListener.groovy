package com.budjb.rabbitmq.test

import com.budjb.rabbitmq.RunningState
import com.budjb.rabbitmq.consumer.ConsumerManager
import com.budjb.rabbitmq.event.ConsumerManagerStartingEvent
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationListener

class ConsumerManagerCheckListener implements ApplicationListener<ConsumerManagerStartingEvent> {
    @Autowired
    ConsumerManager consumerManager

    /**
     * Allowing null is intention here. Tests need to know whether the check actually occurred.
     */
    Boolean started = null

    /**
     * {@inheritDoc}
     */
    @Override
    void onApplicationEvent(ConsumerManagerStartingEvent event) {
        started = consumerManager.getRunningState() == RunningState.RUNNING
    }
}

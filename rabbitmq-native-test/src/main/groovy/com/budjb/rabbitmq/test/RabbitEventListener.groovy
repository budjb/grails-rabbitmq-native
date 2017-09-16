package com.budjb.rabbitmq.test

import com.budjb.rabbitmq.event.RabbitEvent
import org.springframework.context.ApplicationListener

class RabbitEventListener implements ApplicationListener<RabbitEvent> {
    Map<Class<RabbitEvent>, Integer> counter = [:]

    @Override
    void onApplicationEvent(RabbitEvent event) {
        increment(event)
    }

    void increment(RabbitEvent event) {
        Class<RabbitEvent> type = (Class<RabbitEvent>) event.getClass()

        if (!counter.containsKey(type)) {
            counter.put(type, 0)
        }
        counter.put(type, counter.get(type)++)
    }

    int getCount(Class<RabbitEvent> type) {
        if (counter.containsKey(type)) {
            return counter.get(type)
        }
        return 0
    }
}

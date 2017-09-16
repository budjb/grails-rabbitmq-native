package com.budjb.rabbitmq.event

import org.springframework.context.ApplicationEvent

class RabbitEvent extends ApplicationEvent {
    /**
     * Create a new ApplicationEvent.
     *
     * @param source the object on which the event initially occurred (never {@code null})
     */
    RabbitEvent(Object source) {
        super(source)
    }
}

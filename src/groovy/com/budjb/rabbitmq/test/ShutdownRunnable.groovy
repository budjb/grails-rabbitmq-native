package com.budjb.rabbitmq.test

import com.budjb.rabbitmq.RabbitContext

class ShutdownRunnable implements Runnable {
    RabbitContext rabbitContext

    ShutdownRunnable(RabbitContext rabbitContext) {
        this.rabbitContext = rabbitContext
    }

    @Override
    void run() {
        rabbitContext.shutdown()
    }
}

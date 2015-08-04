package com.budjb.rabbitmq.test

import org.apache.log4j.Logger

class SleepingConsumer {
    static rabbitConfig = [
        connection: 'connection1',
        queue: 'sleeping'
    ]

    void handleMessage(Integer sleepTime) {
        if (sleepTime > 0) {
            sleep(sleepTime)
        }
    }
}

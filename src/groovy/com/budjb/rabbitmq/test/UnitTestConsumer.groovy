package com.budjb.rabbitmq.test

class UnitTestConsumer {
    static rabbitConfig = [
        queue: 'test-queue',
        consumers: 5
    ]

    def handleMessage(def body, def context) {

    }

    void onReceive(def context) {

    }

    def onSuccess(def context) {

    }

    def onComplete(def context) {

    }

    def onFailure(def context) {

    }
}

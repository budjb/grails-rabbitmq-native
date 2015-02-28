package com.budjb.rabbitmq.test.helper

interface PersistenceInterceptor {
    void init()

    void flush()

    void destroy()
}

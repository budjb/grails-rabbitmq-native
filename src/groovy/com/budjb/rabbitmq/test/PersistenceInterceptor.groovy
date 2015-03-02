package com.budjb.rabbitmq.test

interface PersistenceInterceptor {
    void init()

    void flush()

    void destroy()
}

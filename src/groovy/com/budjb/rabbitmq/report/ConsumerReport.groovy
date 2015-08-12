package com.budjb.rabbitmq.report

import com.budjb.rabbitmq.RunningState

class ConsumerReport {
    /**
     * Running state of the consumer context.
     */
    RunningState runningState

    /**
     * Number of concurrent threads configured for the consumer.
     */
    int numConfigured

    /**
     * Number of consumers currently processing messages.
     */
    int numProcessing

    /**
     * Number of consumers actively consuming from a queue.
     *
     * This number only reflects the number of channels consuming, some of which may
     * be idle if no message are present to process.
     */
    int numConsuming

    /**
     * Name of the consumer.
     */
    String name

    /**
     * Full name of the consumer, including its package name.
     */
    String fullName

    /**
     * Queue the consumer is consuming from.
     */
    String queue

    /**
     * Percent load.
     */
    float load
}

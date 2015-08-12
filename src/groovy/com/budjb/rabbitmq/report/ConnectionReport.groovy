package com.budjb.rabbitmq.report

import com.budjb.rabbitmq.RunningState

class ConnectionReport {
    /**
     * Running state of the connection.
     */
    RunningState runningState

    /**
     * Name of the connection.
     */
    String name

    /**
     * Connection's host.
     */
    String host

    /**
     * Connection port.
     */
    int port

    /**
     * Connection's virtual host.
     */
    String virtualHost

    /**
     * List of consumers tied to this connection.
     */
    List<ConsumerReport> consumers
}

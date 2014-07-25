package com.budjb.rabbitmq

import com.rabbitmq.client.ShutdownListener
import com.rabbitmq.client.ShutdownSignalException
import org.apache.log4j.Logger

class ChannelShutdownListener implements ShutdownListener {
    Logger log = Logger.getLogger(ChannelShutdownListener)

    void shutdownCompleted(ShutdownSignalException cause) {
        log.info "Channel '${cause.reference}' shutdown", cause
        log.debug cause.reason
    }
}

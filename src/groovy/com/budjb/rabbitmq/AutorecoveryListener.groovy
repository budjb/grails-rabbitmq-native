package com.budjb.rabbitmq

import com.rabbitmq.client.Recoverable
import com.rabbitmq.client.RecoveryListener
import org.apache.log4j.Logger

class AutorecoveryListener implements RecoveryListener {
    Logger log = Logger.getLogger(AutorecoveryListener)

    void handleRecovery(Recoverable recoverable) {
        log.warn "channel ${recoverable} recovered"
    }
}

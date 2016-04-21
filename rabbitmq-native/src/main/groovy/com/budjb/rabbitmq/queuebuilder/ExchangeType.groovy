package com.budjb.rabbitmq.queuebuilder

import org.slf4j.Logger
import org.slf4j.LoggerFactory

enum ExchangeType {
    /**
     * Fanout exchange.
     */
    FANOUT,

    /**
     * Direct exchange.
     */
    DIRECT,

    /**
     * Topic exchange.
     */
    TOPIC,

    /**
     * Headers exchange.
     */
    HEADERS

    /**
     * Logger.
     */
    private static Logger log = LoggerFactory.getLogger(ExchangeType)

    /**
     * Returns the enum that matches the given value regardless of character case.
     *
     * If no match is found, <code>null</code> is returned.
     *
     * @param val
     * @return
     */
    static ExchangeType lookup(String val) {
        if (val == null) {
            return null
        }
        try {
            return valueOf(val.toUpperCase())
        }
        catch (IllegalArgumentException e) {
            log.trace("no MatchType with name '${val}' found", e)
            return null
        }
    }
}

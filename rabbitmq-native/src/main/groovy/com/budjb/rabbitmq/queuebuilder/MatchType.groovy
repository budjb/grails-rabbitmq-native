package com.budjb.rabbitmq.queuebuilder

import org.slf4j.Logger
import org.slf4j.LoggerFactory

enum MatchType {
    /**
     * Header bindings can match any one header.
     */
    ANY,

    /**
     * Header bindings should match all headers.
     */
    ALL

    /**
     * Logger.
     */
    private static Logger log = LoggerFactory.getLogger(MatchType)

    /**
     * Returns the enum that matches the given value regardless of character case.
     *
     * If no match is found, <code>null</code> is returned.
     *
     * @param val
     * @return
     */
    static MatchType lookup(String val) {
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
package com.budjb.rabbitmq.queuebuilder

import com.budjb.rabbitmq.utils.ConfigResolver

trait ConfigurationProperties extends ConfigResolver {

    /**
     * Determines if the minimum requirements of this configuration set have been met and can be considered valid.
     */
    abstract void validate()
}

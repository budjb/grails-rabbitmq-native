package com.budjb.rabbitmq.exception

/**
 * Created with IntelliJ IDEA.
 * User: Michael Rice
 * Twitter: @errr_
 * Website: http://www.errr-online.com/
 * Github: https://github.com/michaelrice
 * Date: 10/22/13
 * Time: 6:52 PM
 */
class MissingConfigurationException extends Exception {
    public MissingConfigurationException(String message) {
        super(message)
    }
}

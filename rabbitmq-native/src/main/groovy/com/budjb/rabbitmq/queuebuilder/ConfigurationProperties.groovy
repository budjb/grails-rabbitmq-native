package com.budjb.rabbitmq.queuebuilder

import org.grails.config.NavigableMap

abstract class ConfigurationProperties {
    /**
     * Parses a configuration option given a class type and possible values.
     *
     * @param clazz
     * @param values
     * @return
     */
    public <T> T parseConfigOption(Class<T> clazz, Object... values) {
        for (Object value : values) {
            if (value == null || value instanceof NavigableMap.NullSafeNavigator || value instanceof ConfigObject) {
                continue
            }

            if (clazz.isAssignableFrom(value.getClass())) {
                return value as T
            }
        }

        return null
    }

    /**
     * Determines if the minimum requirements of this configuration set have been met and can be considered valid.
     */
    abstract void validate()
}

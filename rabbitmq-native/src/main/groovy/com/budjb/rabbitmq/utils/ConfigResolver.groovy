package com.budjb.rabbitmq.utils

import org.grails.config.NavigableMap

/**
 * @since 02/12/2016
 */
trait ConfigResolver {

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
}

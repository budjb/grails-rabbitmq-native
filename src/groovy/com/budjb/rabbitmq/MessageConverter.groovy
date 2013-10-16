package com.budjb.rabbitmq

import java.lang.reflect.ParameterizedType

abstract class MessageConverter<T> {
    /**
     * Returns the class type this converter is responsible for converting.
     *
     * @return
     */
    public Class getType() {
        return ((ParameterizedType)getClass().getGenericSuperclass()).actualTypeArguments[0]
    }

    /**
     * Returns the content type this handler can convert.
     *
     * @return Content-type, or null if a content-type does not exist.
     */
    public abstract String getContentType()

    /**
     * Returns whether the converter can convert the object from its source type to a byte array.
     *
     * @return
     */
    public abstract boolean canConvertFrom()

    /**
     * Returns whether the converter can convert the object from a byte array to its proper type.
     * @return
     */
    public abstract boolean canConvertTo()

    /**
     * Converts a byte array to the object type this converter is responsible for.
     *
     * @param input
     * @return
     */
    public abstract T convertTo(byte[] input)

    /**
     * Converts an object to a byte array.
     *
     * @param input
     * @return
     */
    public abstract byte[] convertFrom(T input)
}

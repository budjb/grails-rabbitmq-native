package com.budjb.rabbitmq

interface MessageConverter<T> {
    /**
     * Returns the class type this converter is responsible for converting.
     *
     * @return
     */
    public Class getType()

    /**
     * Returns whether the converter can convert the object from its source type to a byte array.
     *
     * @return
     */
    public boolean canConvertFrom()

    /**
     * Returns whether the converter can convert the object from a byte array to its proper type.
     * @return
     */
    public boolean canConvertTo()

    /**
     * Converts a byte array to the object type this converter is responsible for.
     *
     * @param input
     * @return
     */
    public T convertTo(byte[] input)

    /**
     * Converts an object to a byte array.
     *
     * @param input
     * @return
     */
    public byte[] convertFrom(T input)
}

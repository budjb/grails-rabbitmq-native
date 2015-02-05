package com.budjb.rabbitmq.converter

import org.apache.log4j.Logger

import com.budjb.rabbitmq.exception.MessageConvertException

class MessageConverterManager {
    /**
     * Logger.
     */
    Logger log = Logger.getLogger(MessageConverterManager)

    /**
     * Registered message converters.
     */
    protected List<MessageConverter> messageConverters = []

    /**
     * Returns the list of registered message converters.
     *
     * @return
     */
    public List<MessageConverter> getMessageConverters() {
        return messageConverters
    }

    /**
     * Registers a message converter.
     *
     * @param messageConverter
     */
    public void registerMessageConverter(MessageConverter messageConverter) {
        messageConverters << messageConverter
    }

    /**
     * Converts a byte array to some other type.
     *
     * @param source Byte array to convert.
     * @return
     * @throws MessageConvertException if there is no converter for the source.
     */
    public Object convertFromBytes(byte[] source) throws MessageConvertException {
        for (MessageConverter converter in messageConverters) {
            if (!converter.canConvertTo()) {
                continue
            }

            try {
                Object converted = converter.convertTo(source)

                if (converted != null) {
                    return converted
                }
            }
            catch (Exception e) {
                log.error("unhandled exception caught from message converter ${converter.class.simpleName}", e)
            }
        }

        throw new MessageConvertException('no message converter found to convert from a byte array')
    }

    /**
     * Converts a byte array to some other type based on the given content type.
     *
     * @param source Byte array to convert.
     * @param contentType
     * @return
     * @throws MessageConvertException if there is no converter for the source.
     */
    public Object convertFromBytes(byte[] source, String contentType) throws MessageConvertException {
        // Find all converters that can handle the content type
        List<MessageConverter> converters = messageConverters.findAll { it.contentType == contentType }

        // If converters are found and it can convert to its type, allow it to do so
        for (MessageConverter converter in converters) {
            if (!converter.canConvertTo()) {
                continue
            }

            try {
                Object converted = converter.convertTo(source)

                if (converted != null) {
                    return converted
                }
            }
            catch (Exception e) {
                log.error("unhandled exception caught from message converter ${converter.class.simpleName}", e)
            }
        }

        throw new MessageConvertException('no message converter found to convert from a byte array')
    }

    /**
     * Converts a given object to a byte array using the message converters.
     *
     * @param source
     * @return
     * @throws MessageConvertException
     */
    public byte[] convertToBytes(Object source) throws MessageConvertException {
        for (MessageConverter converter in messageConverters) {
            if (!converter.canConvertFrom()) {
                continue
            }

            try {
                byte[] converted = converter.convertFrom(source)

                if (converted != null) {
                    return converted
                }
            }
            catch (Exception e) {
                log.error("unhandled exception caught from message converter ${converter.class.simpleName}", e)
            }
        }

        throw new MessageConvertException('no message converter found to convert class ${source.getClass().name} to a byte array')
    }

    /**
     * Resets the message converter manager.
     */
    public void reset() {
        messageConverters = []
    }
}

package ${packageName}

import com.budjb.rabbitmq.converter.MessageConverter

class ${className}MessageConverter extends MessageConverter<${className}> {


    @Override
    String getContentType() {
        null //TODO MimeType this converter handles
    }

    @Override
    boolean canConvertFrom() {
        false // TODO handle conversion from byte[]
    }

    @Override
    boolean canConvertTo() {
        false // TODO handle conversion to byte[]
    }

    @Override
    ${className} convertTo(byte[] input) {
        null // TODO convert byte[] to type
    }

    @Override
    byte[] convertFrom(${className} input) {
        new byte[0] // TODO convert type to byte[]
    }
}

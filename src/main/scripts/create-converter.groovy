description "Creates a new Rabbit-native Message Converter class.", {
    usage "grails create-converter [converter type]"
    argument name: 'Converter Type', description: 'The name of the converter type.'
}

model = model(args[0])
render template: 'Converter.groovy',
       destination: file("grails-app/rabbit-converters/$model.packagePath/${model.simpleName}MessageConverter.groovy"),
       model: model
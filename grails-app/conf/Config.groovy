grails.doc.images = new File('src/docs/images')

log4j = {
    trace 'com.budjb.rabbitmq'
}

rabbitmq {
    enabled = false

    connection = {
        connection(host: 'localhost', username: 'guest', password: 'guest')
    }
    queues = {
        queue(name: 'test-queue', durable: false)
    }
}

grails.doc.images = new File('src/docs/images')

log4j = {
    trace 'com.budjb.rabbitmq'
}

rabbitmq {
    connection = {
        connection(
            host: 'localhost',
            username: 'guest',
            password: 'guest',
            virtualHost: 'test1.rabbitmq.budjb.com',
            isDefault: true,
            name: 'connection1'
        )
        connection(
            host: 'localhost',
            username: 'guest',
            password: 'guest',
            virtualHost: 'test2.rabbitmq.budjb.com',
            name: 'connection2'
        )
    }
    queues = {
        connection('connection1') {
            queue(name: 'reporting', autoDelete: true)
            exchange(name: 'topic-exchange', type: 'topic', autoDelete: true) {
                queue(name: 'topic-queue-1', autoDelete: true, binding: 'com.budjb.#')
                queue(name: 'topic-queue-2', autoDelete: true, binding: 'com.budjb.rabbitmq')
            }
        }
        connection('connection2') {

        }
    }
}

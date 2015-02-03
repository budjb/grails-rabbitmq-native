grails.project.work.dir = 'target'

grails.project.dependency.resolution = {
    inherits 'global'
    log "warn"

    repositories {
        grailsCentral()
        mavenLocal()
        mavenCentral()
    }

    dependencies {
        compile 'com.rabbitmq:amqp-client:3.4.3'
        test 'org.mockito:mockito-all:1.9.5', {
            export = false
        }
        test 'com.cyrusinnovation:mockito-groovy-support:1.3', {
            export = false
        }
    }

    plugins {
        build ":release:2.2.1", {
            export = false
        }
    }
}

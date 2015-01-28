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
    }

    plugins {
        build ":release:2.2.1", {
            export = false
        }
    }
}

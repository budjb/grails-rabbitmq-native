grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"

grails.project.dependency.resolver = "maven"
grails.project.dependency.resolution = {
    inherits "global"
    log "warn"

    repositories {
        grailsCentral()
        mavenCentral()
    }
    dependencies {
        compile 'com.rabbitmq:amqp-client:3.5.4'
    }

    plugins {
        build ":release:3.0.1", {
            export = false
        }
        test ":code-coverage:1.2.7", {
            export = false
        }
        compile ":gpars:0.3"
    }
}

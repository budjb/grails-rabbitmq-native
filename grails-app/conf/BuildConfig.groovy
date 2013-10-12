grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"

grails.project.dependency.resolution = {
    inherits("global") { }
    log "warn"
    repositories {
        grailsCentral()
        mavenCentral()
    }
    dependencies {
        compile 'com.rabbitmq:amqp-client:3.1.4'
    }
    plugins {
        build(":tomcat:$grailsVersion",
              ":release:2.0.3",
              ":rest-client-builder:1.0.2") {
            export = false
        }
    }
}

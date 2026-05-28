package com.tnear.adoptloop

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class AdoptloopServerApplication

fun main(args: Array<String>) {
    runApplication<AdoptloopServerApplication>(*args)
}

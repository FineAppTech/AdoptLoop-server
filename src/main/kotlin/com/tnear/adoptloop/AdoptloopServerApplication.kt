package com.tnear.adoptloop

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class AdoptloopServerApplication

fun main(args: Array<String>) {
    runApplication<AdoptloopServerApplication>(*args)
}

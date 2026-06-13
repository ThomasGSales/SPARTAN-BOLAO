package com.spartan.bolao

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class BolaoApplication

fun main(args: Array<String>) {
    runApplication<BolaoApplication>(*args)
}

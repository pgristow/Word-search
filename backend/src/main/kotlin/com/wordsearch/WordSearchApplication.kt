package com.wordsearch

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class WordSearchApplication

fun main(args: Array<String>) {
    runApplication<WordSearchApplication>(*args)
}

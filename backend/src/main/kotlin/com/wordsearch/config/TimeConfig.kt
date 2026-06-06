package com.wordsearch.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import java.time.Clock

@Configuration
@EnableScheduling
class TimeConfig {
    @Bean
    fun clock(): Clock = Clock.systemUTC()
}

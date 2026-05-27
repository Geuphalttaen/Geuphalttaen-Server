package com.geuphalttaen.api.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor

@Configuration
class AsyncConfig {

    @Bean(name = ["taskExecutor"])
    fun asyncExecutor(): Executor = ThreadPoolTaskExecutor().apply {
        corePoolSize = 2
        maxPoolSize = 4
        queueCapacity = 10
        setThreadNamePrefix("sync-worker-")
        initialize()
    }
}

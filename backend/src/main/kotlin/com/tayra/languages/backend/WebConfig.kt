package com.tayra.languages.backend

import com.tayra.languages.sync.SyncJson
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.json.KotlinSerializationJsonHttpMessageConverter
import java.time.Clock

@Configuration
class WebConfig {
    /** Bodies are JSON as the apps write it, with the same library and settings as the sync protocol. */
    @Bean
    fun kotlinSerialization() = KotlinSerializationJsonHttpMessageConverter(SyncJson)

    @Bean
    fun clock(): Clock = Clock.systemUTC()
}

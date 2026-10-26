package com.tayra.languages.backend

import com.tayra.languages.backend.auth.BrowserTokens
import com.tayra.languages.sync.SyncJson
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.json.KotlinSerializationJsonHttpMessageConverter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import java.time.Clock

@Configuration
class WebConfig {
    /** Bodies are JSON as the apps write it, with the same library and settings as the sync protocol. */
    @Bean
    fun kotlinSerialization() = KotlinSerializationJsonHttpMessageConverter(SyncJson)

    @Bean
    fun clock(): Clock = Clock.systemUTC()

    /**
     * The web app, on its own origin, may call the service and send the sign-in cookie with it.
     * Only the origins named may; none by default, as only the web app needs it.
     */
    @Bean
    fun corsConfigurationSource(@Value("\${tayra.web-origins}") origins: String): CorsConfigurationSource =
        UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/api/**", CorsConfiguration().apply {
                allowedOrigins = origins.split(',').map { it.trim() }.filter { it.isNotEmpty() }
                allowedMethods = listOf("GET", "POST", "PUT", "DELETE")
                allowedHeaders = listOf("Authorization", "Content-Type", BrowserTokens.CLIENT_HEADER)
                exposedHeaders = listOf("Retry-After")
                allowCredentials = true
                maxAge = 3600
            })
        }
}

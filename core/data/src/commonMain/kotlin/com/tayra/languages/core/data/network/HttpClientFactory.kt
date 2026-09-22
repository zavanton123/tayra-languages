package com.tayra.languages.core.data.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header

/** Platform HTTP client with the given configuration applied. */
expect fun platformHttpClient(config: HttpClient.() -> Unit = {}): HttpClient

fun createHttpClient(): HttpClient = platformHttpClient().config {
    install(HttpCache)
    install(HttpTimeout) {
        requestTimeoutMillis = 20_000
        connectTimeoutMillis = 20_000
    }
    defaultRequest {
        header("User-Agent", "TayraLanguages/1.0")
    }
}

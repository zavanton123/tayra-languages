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

/** A client for the account service; library files sent and taken back set longer timeouts of their own. */
fun createAccountHttpClient(): HttpClient = platformHttpClient().config {
    install(HttpTimeout) {
        requestTimeoutMillis = 30_000
        connectTimeoutMillis = 15_000
    }
    defaultRequest {
        header("User-Agent", "TayraLanguages/1.0")
    }
}

/**
 * A client whose requests carry no headers of their own, so a browser sends them without the
 * preflight a server that only answers GETs refuses (WiktApi's OPTIONS is a 404).
 */
fun createPlainHttpClient(): HttpClient = platformHttpClient().config {
    install(HttpTimeout) {
        requestTimeoutMillis = 20_000
        connectTimeoutMillis = 20_000
    }
}

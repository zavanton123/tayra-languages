package com.tayra.languages.core.data.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin

actual fun platformHttpClient(config: HttpClient.() -> Unit): HttpClient = HttpClient(Darwin).apply(config)

actual fun backendUrl(): String = "http://localhost:8085"

package com.tayra.languages.core.data.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp

actual fun platformHttpClient(config: HttpClient.() -> Unit): HttpClient = HttpClient(OkHttp).apply(config)

/** The server on this computer, or the one named by `-Dtayra.backendUrl` or `TAYRA_BACKEND_URL`. */
actual fun backendUrl(): String = System.getProperty("tayra.backendUrl") ?: System.getenv("TAYRA_BACKEND_URL") ?: "http://localhost:8085"

package com.tayra.languages.core.data.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp

actual fun platformHttpClient(config: HttpClient.() -> Unit): HttpClient = HttpClient(OkHttp).apply(config)

/** The server on the computer running the emulator. */
actual fun backendUrl(): String = "http://10.0.2.2:8085"

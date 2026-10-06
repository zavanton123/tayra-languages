package com.tayra.languages.core.data.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp

actual fun platformHttpClient(config: HttpClient.() -> Unit): HttpClient = HttpClient(OkHttp).apply(config)

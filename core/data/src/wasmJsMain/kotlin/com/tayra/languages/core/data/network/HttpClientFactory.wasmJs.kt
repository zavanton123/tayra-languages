package com.tayra.languages.core.data.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.js.Js

actual fun platformHttpClient(config: HttpClient.() -> Unit): HttpClient = HttpClient(Js).apply(config)

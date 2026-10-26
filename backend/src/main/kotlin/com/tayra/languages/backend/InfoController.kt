package com.tayra.languages.backend

import kotlinx.serialization.Serializable
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Clock

/** What the service is and its clock, which the apps compare their own with before stamping changes. */
@Serializable
data class Info(val service: String, val apiVersion: Int, val serverTime: Long)

@RestController
class InfoController(private val clock: Clock) {
    @GetMapping("/api/v1/info")
    fun info() = Info(service = "tayra-languages", apiVersion = 1, serverTime = clock.millis())
}

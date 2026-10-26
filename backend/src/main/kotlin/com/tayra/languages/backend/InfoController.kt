package com.tayra.languages.backend

import com.tayra.languages.sync.Info
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Clock

@RestController
class InfoController(private val clock: Clock) {
    @GetMapping("/api/v1/info")
    fun info() = Info(service = "tayra-languages", apiVersion = 1, serverTime = clock.millis())
}

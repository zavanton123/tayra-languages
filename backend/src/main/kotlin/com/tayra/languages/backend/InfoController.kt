package com.tayra.languages.backend

import com.tayra.languages.sync.Info
import com.tayra.languages.sync.SignInOptions
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Clock

@RestController
class InfoController(
    private val clock: Clock,
    @Value("\${tayra.auth.google.web-client-id}") private val googleWeb: String,
    @Value("\${tayra.auth.google.ios-client-id}") private val googleIos: String,
    @Value("\${tayra.auth.google.desktop-client-id}") private val googleDesktop: String,
    @Value("\${tayra.auth.google.desktop-client-secret}") private val googleDesktopSecret: String,
    @Value("\${tayra.auth.apple.client-ids}") private val apple: String,
) {
    /** The service, its clock, and how the apps may sign in, so a client id set here needs no new app. */
    @GetMapping("/api/v1/info")
    fun info() = Info(
        service = "tayra-languages", apiVersion = 1, serverTime = clock.millis(),
        signIn = SignInOptions(
            googleWebClientId = googleWeb.ifBlank { null },
            googleIosClientId = googleIos.ifBlank { null },
            googleDesktopClientId = googleDesktop.ifBlank { null },
            googleDesktopClientSecret = googleDesktopSecret.ifBlank { null },
            apple = apple.isNotBlank(),
        ),
    )
}

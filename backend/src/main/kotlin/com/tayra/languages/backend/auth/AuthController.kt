package com.tayra.languages.backend.auth

import com.tayra.languages.sync.RegisterRequest
import com.tayra.languages.sync.RefreshRequest
import com.tayra.languages.sync.LoginRequest
import com.tayra.languages.sync.ApiError
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kotlinx.serialization.SerializationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestController
class AuthController(private val auth: AuthService, private val browser: BrowserTokens) {

    @PostMapping("/api/v1/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    fun register(@RequestBody body: RegisterRequest, request: HttpServletRequest, response: HttpServletResponse) =
        browser.hand(auth.register(body), request, response)

    @PostMapping("/api/v1/auth/login")
    fun login(@RequestBody body: LoginRequest, request: HttpServletRequest, response: HttpServletResponse) =
        browser.hand(auth.login(body), request, response)

    @PostMapping("/api/v1/auth/refresh")
    fun refresh(@RequestBody body: RefreshRequest, request: HttpServletRequest, response: HttpServletResponse) =
        browser.hand(auth.refresh(browser.sent(body, request)), request, response)

    @PostMapping("/api/v1/auth/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun logout(@RequestBody body: RefreshRequest, request: HttpServletRequest, response: HttpServletResponse) {
        auth.logout(browser.sent(body, request))
        browser.forget(request, response)
    }
}

/** Every refusal as an [ApiError] body the apps can read. */
@RestControllerAdvice
class ApiErrors {
    @ExceptionHandler(ApiException::class)
    fun refused(e: ApiException) = ResponseEntity.status(e.status).body(ApiError(e.code, e.message.orEmpty()))

    @ExceptionHandler(HttpMessageNotReadableException::class, SerializationException::class)
    fun unreadable(e: Exception) = ResponseEntity.badRequest().body(ApiError("invalid_request", "The request body is not what this call takes"))
}

package com.tayra.languages.backend.auth

import kotlinx.serialization.SerializationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.util.UUID

@RestController
class AuthController(private val auth: AuthService, private val accounts: Accounts) {

    @PostMapping("/api/v1/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    fun register(@RequestBody request: RegisterRequest) = auth.register(request)

    @PostMapping("/api/v1/auth/login")
    fun login(@RequestBody request: LoginRequest) = auth.login(request)

    @PostMapping("/api/v1/auth/refresh")
    fun refresh(@RequestBody request: RefreshRequest) = auth.refresh(request)

    @PostMapping("/api/v1/auth/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun logout(@RequestBody request: RefreshRequest) = auth.logout(request)

    /** The signed-in account. */
    @GetMapping("/api/v1/me")
    fun me(@AuthenticationPrincipal jwt: Jwt): Account {
        val user = accounts.byId(UUID.fromString(jwt.subject)) ?: throw SignedOut()
        return Account(user.id.toString(), user.email, user.emailVerified)
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

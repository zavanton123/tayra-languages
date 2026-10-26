package com.tayra.languages.backend.auth

import com.tayra.languages.sync.RegisterRequest
import com.tayra.languages.sync.RefreshRequest
import com.tayra.languages.sync.LoginRequest
import com.tayra.languages.sync.ApiError
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
class AuthController(private val auth: AuthService) {

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
}

/** Every refusal as an [ApiError] body the apps can read. */
@RestControllerAdvice
class ApiErrors {
    @ExceptionHandler(ApiException::class)
    fun refused(e: ApiException) = ResponseEntity.status(e.status).body(ApiError(e.code, e.message.orEmpty()))

    @ExceptionHandler(HttpMessageNotReadableException::class, SerializationException::class)
    fun unreadable(e: Exception) = ResponseEntity.badRequest().body(ApiError("invalid_request", "The request body is not what this call takes"))
}

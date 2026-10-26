package com.tayra.languages.backend.auth

import kotlinx.serialization.Serializable

/** The device signing in, so each one keeps its own sign-in and can be listed and signed out. */
@Serializable
data class DeviceInfo(val id: String, val name: String = "", val platform: String = "")

@Serializable
data class RegisterRequest(val email: String, val password: String, val device: DeviceInfo)

@Serializable
data class LoginRequest(val email: String, val password: String, val device: DeviceInfo)

@Serializable
data class RefreshRequest(val refreshToken: String)

/** What a device keeps after signing in: a short-lived access token and the refresh token that renews it. */
@Serializable
data class Tokens(val accessToken: String, val refreshToken: String, val expiresIn: Long, val user: Account)

@Serializable
data class Account(val id: String, val email: String, val emailVerified: Boolean)

/** A failure the apps can act on: [error] is a fixed code, [message] says it in English. */
@Serializable
data class ApiError(val error: String, val message: String)

/** A refusal with its HTTP status and fixed code. */
open class ApiException(val status: Int, val code: String, message: String) : RuntimeException(message)

class EmailTaken : ApiException(409, "email_taken", "An account with this email exists already")

class BadCredentials : ApiException(401, "bad_credentials", "Wrong email or password")

class InvalidRequest(message: String) : ApiException(400, "invalid_request", message)

class SignedOut : ApiException(401, "signed_out", "This device is signed out; sign in again")

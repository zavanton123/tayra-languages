package com.tayra.languages.backend.auth

import com.tayra.languages.sync.ApiErrors

/** A refusal with its HTTP status and fixed code. */
open class ApiException(val status: Int, val code: String, message: String) : RuntimeException(message)

class EmailTaken : ApiException(409, ApiErrors.EMAIL_TAKEN, "An account with this email exists already")

class BadCredentials : ApiException(401, ApiErrors.BAD_CREDENTIALS, "Wrong email or password")

class InvalidRequest(message: String) : ApiException(400, ApiErrors.INVALID_REQUEST, message)

class SignedOut : ApiException(401, ApiErrors.SIGNED_OUT, "This device is signed out; sign in again")

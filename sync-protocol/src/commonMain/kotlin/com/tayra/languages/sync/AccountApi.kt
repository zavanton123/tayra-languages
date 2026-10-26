package com.tayra.languages.sync

import kotlinx.serialization.Serializable

// The account service's requests and answers, as the apps and the backend both read and write them.

/** What the service is and its clock, which the apps compare their own with before stamping changes. */
@Serializable
data class Info(val service: String, val apiVersion: Int, val serverTime: Long)

/** The device signing in, so each one keeps its own sign-in and can be listed and signed out. */
@Serializable
data class DeviceInfo(val id: String, val name: String = "", val platform: String = "")

@Serializable
data class RegisterRequest(val email: String, val password: String, val device: DeviceInfo)

@Serializable
data class LoginRequest(val email: String, val password: String, val device: DeviceInfo)

/** An ID token from Google or Apple, obtained by the app itself. */
@Serializable
data class ProviderSignInRequest(val idToken: String, val device: DeviceInfo)

@Serializable
data class RefreshRequest(val refreshToken: String)

/** What a device keeps after signing in: a short-lived access token and the refresh token that renews it. */
@Serializable
data class Tokens(val accessToken: String, val refreshToken: String, val expiresIn: Long, val user: Account)

@Serializable
data class Account(val id: String, val email: String, val emailVerified: Boolean)

@Serializable
data class CodeRequest(val code: String)

@Serializable
data class ForgotRequest(val email: String)

@Serializable
data class ResetRequest(val email: String, val code: String, val password: String)

/** Whether the account may sync and back up, until when, and what it was bought through. */
@Serializable
data class Subscription(val active: Boolean, val activeUntil: Long? = null, val source: String? = null)

@Serializable
data class Me(
    val id: String,
    val email: String,
    val emailVerified: Boolean,
    val hasPassword: Boolean,
    val providers: List<String>,
    val subscription: Subscription,
)

@Serializable
data class Device(val id: String, val name: String, val platform: String, val lastUsedAt: Long, val current: Boolean)

@Serializable
data class CloudBackup(val id: String, val name: String, val deviceName: String, val createdAt: Long, val sizeBytes: Long)

/** A failure the apps can act on: [error] is one of [ApiErrors], [message] says it in English. */
@Serializable
data class ApiError(val error: String, val message: String)

/** The codes of [ApiError]. */
/** A purchase the iOS app made with StoreKit, as the signed transaction StoreKit gave it. */
@Serializable
data class AppStorePurchase(val signedTransaction: String)

/** A purchase the Android app made with Google Play Billing: the subscription bought and its purchase token. */
@Serializable
data class GooglePlayPurchase(val productId: String, val purchaseToken: String)

object ApiErrors {
    const val INVALID_REQUEST = "invalid_request"
    const val EMAIL_TAKEN = "email_taken"
    const val BAD_CREDENTIALS = "bad_credentials"
    const val SIGNED_OUT = "signed_out"
    const val WRONG_CODE = "wrong_code"
    const val BAD_ID_TOKEN = "bad_id_token"
    const val PROVIDER_NOT_SET_UP = "provider_not_set_up"
    const val SUBSCRIPTION_REQUIRED = "subscription_required"
    const val NOT_FOUND = "not_found"
    const val TOO_LARGE = "too_large"
    const val RATE_LIMITED = "rate_limited"
}

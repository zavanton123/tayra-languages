package com.tayra.languages.core.domain.account

import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Instant

/** The account a device is signed in to, as the account service has it. */
data class AccountInfo(
    val email: String,
    val emailVerified: Boolean,
    val hasPassword: Boolean,
    /** "google", "apple": the providers the account can sign in with besides a password. */
    val providers: List<String>,
    val plan: Plan,
)

/** Whether the account may sync and back up to the cloud, until when, and what it was bought through. */
data class Plan(val active: Boolean, val activeUntil: Instant? = null, val source: String? = null)

/** A device signed in to the account; [current] is this one. */
data class SignedInDevice(val id: String, val name: String, val platform: String, val lastUsedAt: Instant, val current: Boolean)

/** A library file kept in the cloud. */
data class CloudBackupInfo(val id: String, val name: String, val deviceName: String, val createdAt: Instant, val sizeBytes: Long)

/** Why something to do with the account did not happen. */
enum class AccountProblem {
    /** This build has no account service to talk to. */
    NOT_SET_UP,
    /** The service could not be reached. */
    OFFLINE,
    EMAIL_TAKEN,
    BAD_CREDENTIALS,
    WRONG_CODE,
    /** The device was signed out, here or from another device. */
    SIGNED_OUT,
    SUBSCRIPTION_REQUIRED,
    TOO_LARGE,
    NOT_FOUND,
    INVALID,
    SERVER,
}

class AccountException(val problem: AccountProblem, message: String) : Exception(message)

/**
 * Signing this device in and out of a Tayra Languages account, and what the account holds. Each
 * call throws [AccountException]; [signedIn] is the email of the account the device is signed in to.
 */
interface AccountRepository {
    /** Whether this build can reach an account service at all. */
    val available: Boolean

    val signedIn: StateFlow<String?>

    suspend fun register(email: String, password: String): AccountInfo

    suspend fun signIn(email: String, password: String): AccountInfo

    /** Signs this device out; the account and its other devices are untouched. */
    suspend fun signOut()

    suspend fun account(): AccountInfo

    suspend fun sendConfirmationCode()

    suspend fun confirmEmail(code: String)

    suspend fun forgotPassword(email: String)

    suspend fun resetPassword(email: String, code: String, password: String)

    suspend fun devices(): List<SignedInDevice>

    suspend fun signOutDevice(id: String)

    /** Deletes the account and everything the service keeps for it; this device is signed out. */
    suspend fun deleteAccount()
}

/** The account's library files in the cloud. */
interface CloudBackupStore {
    suspend fun list(): List<CloudBackupInfo>

    suspend fun upload(name: String, content: ByteArray): CloudBackupInfo

    suspend fun download(id: String): ByteArray

    suspend fun delete(id: String)
}

/** What one sync did: the records sent, the records received, and how many of those changed this device. */
data class SyncSummary(val sent: Int, val received: Int, val changed: Int)

/** Brings this device and the account to the same library: sends what changed here, takes what changed elsewhere. */
interface LibrarySync {
    /** Throws [AccountException] when the account cannot be reached or may not sync. */
    suspend fun syncNow(): SyncSummary

    /**
     * Whether the next sync would combine two libraries: this device has not synced with the account
     * yet, and both hold words, texts or reading of their own. Throws as [syncNow] does.
     */
    suspend fun needsCombining(): Boolean
}

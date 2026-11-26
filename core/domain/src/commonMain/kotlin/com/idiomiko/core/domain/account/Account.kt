package com.idiomiko.core.domain.account

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlin.time.Instant

/** The account a device is signed in to, as the account service has it. */
data class AccountInfo(
    val email: String,
    val emailVerified: Boolean,
    val hasPassword: Boolean,
    /** "google", "apple": the providers the account can sign in with besides a password. */
    val providers: List<String>,
    val plan: Plan,
    /** The account's id, which a store purchase carries so the service knows whose it is. */
    val id: String = "",
    /** The name the account's Google sign-in gave; null without one. */
    val name: String? = null,
    /** Which picture the account has, changing with it; null without one. */
    val picture: String? = null,
    /** The picture is one the owner uploaded, which they can remove to show Google's again, if any. */
    val ownPicture: Boolean = false,
)

/** Who this device is signed in as, for the app's bar and the account page: kept on the device, so it shows offline too. */
data class AccountProfile(
    val email: String,
    val name: String? = null,
    /** Which picture the account has ([AccountRepository.picture] fetches it); null without one. */
    val picture: String? = null,
)

private val NO_PROFILE: StateFlow<AccountProfile?> = MutableStateFlow<AccountProfile?>(null).asStateFlow()

/** Whether the account may sync and back up to the cloud, until when, and what it was bought through. */
data class Plan(
    val active: Boolean,
    val activeUntil: Instant? = null,
    val source: String? = null,
    /** Where to subscribe in the browser; only for the desktop and web apps, as the stores sell in theirs. */
    val checkoutUrl: String? = null,
    /** Where the store the subscription was bought in lets it be changed or cancelled. */
    val manageUrl: String? = null,
)

/** A device signed in to the account; [current] is this one. */
data class SignedInDevice(val id: String, val name: String, val platform: String, val lastUsedAt: Instant, val current: Boolean)

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
    /** Too many requests from this address for now: a minute later they are taken again. */
    BUSY,
    /** The address was sent as many codes as it may be for now: more go to it an hour later. */
    TOO_MANY_CODES,
    NOT_FOUND,
    INVALID,
    SERVER,
}

class AccountException(val problem: AccountProblem, message: String) : Exception(message)

/**
 * Signing this device in and out of a Idiomiko account, and what the account holds. Each
 * call throws [AccountException]; [signedIn] is the email of the account the device is signed in to.
 */
interface AccountRepository {
    /** Whether this build can reach an account service at all. */
    val available: Boolean

    val signedIn: StateFlow<String?>

    /** The account signed in to, as this device last heard of it; null when signed out. */
    val profile: StateFlow<AccountProfile?> get() = NO_PROFILE

    /** The image of the account's picture (PNG or JPEG), or null when it has none or it cannot be fetched now. */
    suspend fun picture(): ByteArray? = null

    /**
     * In the web app, which shares its sign-in with the service's own pages: takes up the sign-in the
     * browser has already, when this app is not signed in itself. True when it did; elsewhere false.
     */
    suspend fun joinSharedSignIn(): Boolean = false

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

    /** Makes [image], a PNG or JPEG, the account's picture in place of any other; [profile] follows. */
    suspend fun uploadPicture(image: ByteArray): Unit = throw AccountException(AccountProblem.NOT_SET_UP, "Pictures cannot be changed here")

    /** Removes the picture the owner uploaded, showing Google's again where the account has one; [profile] follows. */
    suspend fun removePicture(): Unit = throw AccountException(AccountProblem.NOT_SET_UP, "Pictures cannot be changed here")

    /** Hands the account service a purchase the store just made, or one made before, for it to check and take. */
    suspend fun reportPurchase(receipt: StoreReceipt)

    /** How the service lets apps sign in besides a password; none when it cannot be asked. */
    suspend fun signInClients(): SignInClients

    /** Signs in with an ID token [provider] gave this device; the account is made the first time. */
    suspend fun signInWith(provider: SignInProvider, idToken: String): AccountInfo
}

/** What one sync did: the records sent, the records received, and how many of those changed this device. */
data class SyncSummary(val sent: Int, val received: Int, val changed: Int)

/** What a sync is doing: sending this device's records, taking the account's, or applying them here. */
enum class SyncPhase { SENDING, RECEIVING, APPLYING }

/** How far a sync has got in its [phase]: [done] records, of [total] when it is known. */
data class SyncProgress(val phase: SyncPhase, val done: Int = 0, val total: Int? = null)

/** Brings this device and the account to the same library: sends what changed here, takes what changed elsewhere. */
interface LibrarySync {
    /** Says how far it has got to [progress] as it goes. Throws [AccountException] when the account cannot be reached or may not sync. */
    suspend fun syncNow(progress: (SyncProgress) -> Unit = {}): SyncSummary

    /**
     * Whether the next sync would combine two libraries: this device has not synced with the account
     * yet, and both hold words, texts or reading of their own. Throws as [syncNow] does.
     */
    suspend fun needsCombining(): Boolean

    /**
     * Whether this device holds changes the account has not been sent yet, said again whenever the
     * library or the settings that follow the account change: what the next sync would send. True
     * until this device has synced with the account signed in to. What a sync writes here, taken from
     * other devices, is not a change to send.
     */
    fun unsent(): Flow<Boolean> = flowOf(false)

    /** When this device last synced with the account signed in to, kept across restarts; null before its first sync. */
    fun lastSynced(): Instant? = null
}

enum class SignInProvider { GOOGLE, APPLE }

/** The Google clients the service takes from each kind of app, and whether it takes Sign in with Apple. */
data class SignInClients(
    val googleWeb: String? = null,
    val googleIos: String? = null,
    val googleDesktop: String? = null,
    val googleDesktopSecret: String? = null,
    val apple: Boolean = false,
)

/** Thrown when a provider could not sign the person in; [message] says why. */
class IdentityException(message: String) : Exception(message)

/** The platform's way to sign in with Google or Apple: an ID token from the provider, for the service to check. */
interface IdentitySignIn {
    /** The providers this app can sign in with, given what the service takes. */
    fun offers(clients: SignInClients): List<SignInProvider>

    /** An ID token from [provider] for the person at the device; null when they gave up. Throws [IdentityException]. */
    suspend fun idToken(provider: SignInProvider, clients: SignInClients): String?
}

package com.tayra.languages.android

import android.app.Activity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.tayra.languages.core.domain.account.IdentityException
import com.tayra.languages.core.domain.account.IdentitySignIn
import com.tayra.languages.core.domain.account.SignInClients
import com.tayra.languages.core.domain.account.SignInProvider

/**
 * Signing in with Google on Android: Credential Manager's "Sign in with Google" sheet, which gives an
 * ID token for the service's web client, the one Google issues Android apps' tokens to.
 */
class AndroidIdentitySignIn(private val activity: () -> Activity?) : IdentitySignIn {

    override fun offers(clients: SignInClients) = listOfNotNull(SignInProvider.GOOGLE.takeIf { clients.googleWeb != null })

    override suspend fun idToken(provider: SignInProvider, clients: SignInClients): String? {
        val serverClient = clients.googleWeb?.takeIf { provider == SignInProvider.GOOGLE } ?: throw IdentityException("This app cannot sign in with $provider")
        val activity = activity() ?: throw IdentityException("Open the app to sign in")
        val request = GetCredentialRequest.Builder().addCredentialOption(GetSignInWithGoogleOption.Builder(serverClient).build()).build()
        val credential = try {
            CredentialManager.create(activity).getCredential(activity, request).credential
        } catch (_: GetCredentialCancellationException) {
            return null
        } catch (e: GetCredentialException) {
            throw IdentityException(e.message ?: "Google did not sign you in")
        }
        if (credential !is CustomCredential || credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            throw IdentityException("Google gave no ID token")
        }
        return GoogleIdTokenCredential.createFrom(credential.data).idToken
    }
}

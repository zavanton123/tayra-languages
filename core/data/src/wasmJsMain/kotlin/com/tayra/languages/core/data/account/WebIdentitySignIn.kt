package com.tayra.languages.core.data.account

import com.tayra.languages.core.domain.account.IdentityException
import com.tayra.languages.core.domain.account.IdentitySignIn
import com.tayra.languages.core.domain.account.SignInClients
import com.tayra.languages.core.domain.account.SignInProvider
import kotlinx.coroutines.await
import kotlin.js.Promise

/**
 * Google's own "Continue with Google" button, in a sheet over the app (which draws on a canvas, where
 * Google's button cannot be), loading Google Identity Services first; its credential is the ID token.
 * The sheet's ✕ gives up.
 */
private fun googleIdToken(clientId: String): Promise<JsString?> = js(
    """new Promise(function (resolve, reject) {
        function start() {
            var sheet = document.createElement('div');
            function done(token) { sheet.remove(); resolve(token); }
            google.accounts.id.initialize({ client_id: clientId, callback: function (answer) { done(answer.credential); } });
            sheet.style.cssText = 'position:fixed;inset:0;background:rgba(0,0,0,.4);display:flex;align-items:center;justify-content:center;z-index:2147483647';
            var box = document.createElement('div');
            box.style.cssText = 'position:relative;background:#fff;padding:40px 32px 32px;border-radius:12px;box-shadow:0 8px 32px rgba(0,0,0,.25)';
            var close = document.createElement('button');
            close.textContent = '✕';
            close.setAttribute('aria-label', 'Close');
            close.style.cssText = 'position:absolute;top:8px;right:8px;border:0;background:none;font-size:18px;cursor:pointer;color:#444';
            close.onclick = function () { done(null); };
            var slot = document.createElement('div');
            box.appendChild(close);
            box.appendChild(slot);
            sheet.appendChild(box);
            document.body.appendChild(sheet);
            google.accounts.id.renderButton(slot, { type: 'standard', theme: 'outline', size: 'large', text: 'continue_with' });
        }
        if (window.google && google.accounts && google.accounts.id) return start();
        var script = document.createElement('script');
        script.src = 'https://accounts.google.com/gsi/client';
        script.async = true;
        script.onload = start;
        script.onerror = function () { reject(new Error('Google sign-in could not be loaded')); };
        document.head.appendChild(script);
    })""",
)

/** Signing in with Google in the web app, with Google Identity Services and the service's web client. */
class WebIdentitySignIn : IdentitySignIn {

    override fun offers(clients: SignInClients) = listOfNotNull(SignInProvider.GOOGLE.takeIf { clients.googleWeb != null })

    override suspend fun idToken(provider: SignInProvider, clients: SignInClients): String? {
        val clientId = clients.googleWeb?.takeIf { provider == SignInProvider.GOOGLE } ?: throw IdentityException("This app cannot sign in with $provider")
        return try {
            googleIdToken(clientId).await<JsString?>()?.toString()
        } catch (e: Throwable) {
            throw IdentityException(e.message ?: "Google sign-in could not be loaded")
        }
    }
}

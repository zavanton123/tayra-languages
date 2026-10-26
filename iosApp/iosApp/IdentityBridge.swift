import AuthenticationServices
import Shared
import UIKit

/// Apple's AuthenticationServices, handed to the shared code as `IdentityBridge`: Google's sign-in
/// page in a web authentication session, and Sign in with Apple.
final class AuthenticationServicesBridge: NSObject, IdentityBridge, ASWebAuthenticationPresentationContextProviding,
    ASAuthorizationControllerDelegate, ASAuthorizationControllerPresentationContextProviding {
    private var session: ASWebAuthenticationSession?
    private var controller: ASAuthorizationController?
    private var appleCallback: ((String?, String?) -> Void)?

    func authenticate(url: String, callbackScheme: String, callback: @escaping (String?, String?) -> Void) {
        DispatchQueue.main.async {
            guard let address = URL(string: url) else { return callback(nil, "Not an address: \(url)") }
            let session = ASWebAuthenticationSession(url: address, callbackURLScheme: callbackScheme) { [weak self] result, error in
                self?.session = nil
                if let error = error as? ASWebAuthenticationSessionError, error.code == .canceledLogin { return callback(nil, nil) }
                if let error { return callback(nil, error.localizedDescription) }
                callback(result?.absoluteString, nil)
            }
            session.presentationContextProvider = self
            self.session = session
            session.start()
        }
    }

    func appleIdToken(callback: @escaping (String?, String?) -> Void) {
        DispatchQueue.main.async {
            let request = ASAuthorizationAppleIDProvider().createRequest()
            request.requestedScopes = [.email]
            let controller = ASAuthorizationController(authorizationRequests: [request])
            controller.delegate = self
            controller.presentationContextProvider = self
            self.appleCallback = callback
            self.controller = controller
            controller.performRequests()
        }
    }

    func authorizationController(controller: ASAuthorizationController, didCompleteWithAuthorization authorization: ASAuthorization) {
        let token = (authorization.credential as? ASAuthorizationAppleIDCredential)?.identityToken.flatMap { String(data: $0, encoding: .utf8) }
        finishApple(token, token == nil ? "Apple gave no identity token" : nil)
    }

    func authorizationController(controller: ASAuthorizationController, didCompleteWithError error: Error) {
        if let error = error as? ASAuthorizationError, error.code == .canceled { return finishApple(nil, nil) }
        finishApple(nil, error.localizedDescription)
    }

    private func finishApple(_ token: String?, _ error: String?) {
        let callback = appleCallback
        appleCallback = nil
        controller = nil
        callback?(token, error)
    }

    func presentationAnchor(for session: ASWebAuthenticationSession) -> ASPresentationAnchor { window() }

    func presentationAnchor(for controller: ASAuthorizationController) -> ASPresentationAnchor { window() }

    private func window() -> ASPresentationAnchor {
        UIApplication.shared.connectedScenes.compactMap { ($0 as? UIWindowScene)?.keyWindow }.first ?? ASPresentationAnchor()
    }
}

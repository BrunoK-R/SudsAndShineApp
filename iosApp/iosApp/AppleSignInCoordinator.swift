import AuthenticationServices
import ComposeApp
import Foundation
import UIKit

@MainActor
final class AppleSignInCoordinator: NSObject {
    static let shared = AppleSignInCoordinator()

    private var pendingRequest: PendingRequest?
    private var authorizationController: ASAuthorizationController?

    private override init() { super.init() }

    func configureKotlinBridge() {
        let enabled = (Bundle.main.object(forInfoDictionaryKey: "SudsAppleSignInEnabled") as? String) == "YES"
        guard enabled else {
            IosAppleSignInKt.configureIosAppleSignIn(signInHandler: nil)
            return
        }

        IosAppleSignInKt.configureIosAppleSignIn { onCredential, onError, onCancelled in
            self.signIn(
                onCredential: { token, nonce, name in _ = onCredential(token, nonce, name) },
                onError: { message in _ = onError(message) },
                onCancelled: { _ = onCancelled() }
            )
        }
    }

    private func signIn(
        onCredential: @escaping (String, String, String?) -> Void,
        onError: @escaping (String) -> Void,
        onCancelled: @escaping () -> Void
    ) {
        guard pendingRequest == nil else { return }
        guard let window = UIApplication.shared.connectedScenes
            .compactMap({ $0 as? UIWindowScene })
            .filter({ $0.activationState == .foregroundActive })
            .flatMap(\.windows)
            .first(where: \.isKeyWindow) else {
            onError("Não foi possível apresentar o início de sessão Apple.")
            return
        }

        do {
            let nonce = try AppleSignInNonce.generate()
            let state = UUID().uuidString
            let request = ASAuthorizationAppleIDProvider().createRequest()
            request.requestedScopes = [.fullName, .email]
            request.nonce = AppleSignInNonce.sha256(nonce)
            request.state = state
            let controller = ASAuthorizationController(authorizationRequests: [request])
            pendingRequest = PendingRequest(
                nonce: nonce, state: state, window: window,
                onCredential: onCredential, onError: onError, onCancelled: onCancelled
            )
            authorizationController = controller
            controller.delegate = self
            controller.presentationContextProvider = self
            controller.performRequests()
        } catch {
            onError("Não foi possível preparar o início de sessão Apple. Tente novamente.")
        }
    }

    private func takePendingRequest(for controller: ASAuthorizationController) -> PendingRequest? {
        guard controller === authorizationController else { return nil }
        let request = pendingRequest
        pendingRequest = nil
        authorizationController = nil
        return request
    }

    private struct PendingRequest {
        let nonce: String
        let state: String
        let window: UIWindow
        let onCredential: (String, String, String?) -> Void
        let onError: (String) -> Void
        let onCancelled: () -> Void
    }
}

extension AppleSignInCoordinator: ASAuthorizationControllerDelegate, ASAuthorizationControllerPresentationContextProviding {
    func presentationAnchor(for controller: ASAuthorizationController) -> ASPresentationAnchor {
        // The window is retained for the lifetime of this authorization request.
        pendingRequest?.window ?? ASPresentationAnchor()
    }

    func authorizationController(
        controller: ASAuthorizationController,
        didCompleteWithAuthorization authorization: ASAuthorization
    ) {
        guard let request = takePendingRequest(for: controller) else { return }
        guard let credential = authorization.credential as? ASAuthorizationAppleIDCredential,
              credential.state == request.state,
              let data = credential.identityToken,
              let token = String(data: data, encoding: .utf8), !token.isEmpty else {
            request.onError("Não foi possível validar a sessão Apple. Tente novamente.")
            return
        }

        let name = credential.fullName.map {
            PersonNameComponentsFormatter().string(from: $0).trimmingCharacters(in: .whitespacesAndNewlines)
        }
        request.onCredential(token, request.nonce, name?.isEmpty == false ? name : nil)
    }

    func authorizationController(controller: ASAuthorizationController, didCompleteWithError error: Error) {
        guard let request = takePendingRequest(for: controller) else { return }
        if let error = error as? ASAuthorizationError, error.code == .canceled {
            request.onCancelled()
        } else {
            request.onError("Não foi possível iniciar sessão com Apple. Tente novamente.")
        }
    }
}

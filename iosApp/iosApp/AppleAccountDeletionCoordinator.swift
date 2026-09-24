import AuthenticationServices
import ComposeApp
import Foundation
import UIKit

@MainActor
final class AppleAccountDeletionCoordinator: NSObject {
    static let shared = AppleAccountDeletionCoordinator()

    private var pendingRequest: PendingRequest?
    private var authorizationController: ASAuthorizationController?

    private override init() { super.init() }

    func configureKotlinBridge() {
        let enabled = (Bundle.main.object(forInfoDictionaryKey: "SudsAppleSignInEnabled") as? String) == "YES"
        guard enabled else {
            IosAppleAccountDeletionKt.configureIosAppleAccountDeletion(authorizationHandler: nil)
            return
        }

        IosAppleAccountDeletionKt.configureIosAppleAccountDeletion {
            onAuthorizationCode, onError, onCancelled in
            self.authorize(
                onAuthorizationCode: { code in _ = onAuthorizationCode(code) },
                onError: { message in _ = onError(message) },
                onCancelled: { _ = onCancelled() }
            )
        }
    }

    private func authorize(
        onAuthorizationCode: @escaping (String) -> Void,
        onError: @escaping (String) -> Void,
        onCancelled: @escaping () -> Void
    ) {
        guard pendingRequest == nil else { return }
        guard let window = UIApplication.shared.connectedScenes
            .compactMap({ $0 as? UIWindowScene })
            .filter({ $0.activationState == .foregroundActive })
            .flatMap(\.windows)
            .first(where: \.isKeyWindow) else {
            onError("Não foi possível apresentar a confirmação Apple.")
            return
        }

        let state = UUID().uuidString
        let request = ASAuthorizationAppleIDProvider().createRequest()
        request.state = state
        let controller = ASAuthorizationController(authorizationRequests: [request])
        pendingRequest = PendingRequest(
            state: state,
            window: window,
            onAuthorizationCode: onAuthorizationCode,
            onError: onError,
            onCancelled: onCancelled
        )
        authorizationController = controller
        controller.delegate = self
        controller.presentationContextProvider = self
        controller.performRequests()
    }

    private func takePendingRequest(for controller: ASAuthorizationController) -> PendingRequest? {
        guard controller === authorizationController else { return nil }
        let request = pendingRequest
        pendingRequest = nil
        authorizationController = nil
        return request
    }

    private struct PendingRequest {
        let state: String
        let window: UIWindow
        let onAuthorizationCode: (String) -> Void
        let onError: (String) -> Void
        let onCancelled: () -> Void
    }
}

extension AppleAccountDeletionCoordinator:
    ASAuthorizationControllerDelegate,
    ASAuthorizationControllerPresentationContextProviding {
    func presentationAnchor(for controller: ASAuthorizationController) -> ASPresentationAnchor {
        pendingRequest?.window ?? ASPresentationAnchor()
    }

    func authorizationController(
        controller: ASAuthorizationController,
        didCompleteWithAuthorization authorization: ASAuthorization
    ) {
        guard let request = takePendingRequest(for: controller) else { return }
        guard let credential = authorization.credential as? ASAuthorizationAppleIDCredential,
              credential.state == request.state,
              let data = credential.authorizationCode,
              let code = String(data: data, encoding: .utf8), !code.isEmpty else {
            request.onError("Não foi possível confirmar a conta Apple. Tente novamente.")
            return
        }
        request.onAuthorizationCode(code)
    }

    func authorizationController(controller: ASAuthorizationController, didCompleteWithError error: Error) {
        guard let request = takePendingRequest(for: controller) else { return }
        if let error = error as? ASAuthorizationError, error.code == .canceled {
            request.onCancelled()
        } else {
            request.onError("Não foi possível confirmar a conta Apple. Tente novamente.")
        }
    }
}

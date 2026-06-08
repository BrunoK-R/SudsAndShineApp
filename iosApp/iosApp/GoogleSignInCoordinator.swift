import Foundation
import UIKit
import GoogleSignIn
import ComposeApp

final class GoogleSignInCoordinator {
    static let shared = GoogleSignInCoordinator()

    private init() {}

    func configureKotlinBridge() {
        guard isConfigured else {
            IosGoogleSignInKt.configureIosGoogleSignIn(signInHandler: nil)
            return
        }

        IosGoogleSignInKt.configureIosGoogleSignIn { onIdToken, onError in
            self.signIn(
                onIdToken: { idToken in
                    _ = onIdToken(idToken)
                },
                onError: { message in
                    _ = onError(message)
                }
            )
        }
    }

    func handleOpenURL(_ url: URL) -> Bool {
        GIDSignIn.sharedInstance.handle(url)
    }

    private var isConfigured: Bool {
        Bundle.main.nonPlaceholderInfoValue("GIDClientID") != nil &&
            Bundle.main.nonPlaceholderInfoValue("GIDServerClientID") != nil &&
            Bundle.main.nonPlaceholderInfoValue("GIDReversedClientID") != nil
    }

    private func signIn(
        onIdToken: @escaping (String) -> Void,
        onError: @escaping (String) -> Void
    ) {
        guard let presentingViewController = topViewController() else {
            onError("Não foi possível apresentar o início de sessão Google.")
            return
        }

        GIDSignIn.sharedInstance.signIn(withPresenting: presentingViewController) { result, error in
            if error != nil {
                onError("Não foi possível iniciar sessão com Google. Tente novamente.")
                return
            }

            guard let idToken = result?.user.idToken?.tokenString, !idToken.isEmpty else {
                onError("Não foi possível obter a sessão Google. Tente novamente.")
                return
            }

            onIdToken(idToken)
        }
    }

    private func topViewController() -> UIViewController? {
        let rootViewController = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap(\.windows)
            .first { $0.isKeyWindow }?
            .rootViewController

        return topViewController(from: rootViewController)
    }

    private func topViewController(from rootViewController: UIViewController?) -> UIViewController? {
        if let navigationController = rootViewController as? UINavigationController {
            return topViewController(from: navigationController.visibleViewController)
        }

        if let tabBarController = rootViewController as? UITabBarController {
            return topViewController(from: tabBarController.selectedViewController)
        }

        if let presentedViewController = rootViewController?.presentedViewController {
            return topViewController(from: presentedViewController)
        }

        return rootViewController
    }
}

private extension Bundle {
    func nonPlaceholderInfoValue(_ key: String) -> String? {
        guard let value = object(forInfoDictionaryKey: key) as? String else {
            return nil
        }

        let trimmedValue = value.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmedValue.isEmpty, !trimmedValue.contains("$(") else {
            return nil
        }

        return trimmedValue
    }
}

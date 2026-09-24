import Foundation
import UIKit
import GoogleSignIn
import ComposeApp

@MainActor
final class GoogleSignInCoordinator {
    static let shared = GoogleSignInCoordinator()

    private var pendingRequest: PendingRequest?

    private init() {}

    func configureKotlinBridge() {
        let configurationResult = GoogleSignInConfiguration.load()
        guard configurationResult.issues.isEmpty, let configuration = configurationResult.configuration else {
            logMissingConfiguration(configurationResult.issues)
            IosGoogleSignInKt.configureIosGoogleSignIn(signInHandler: nil)
            return
        }

        GIDSignIn.sharedInstance.configuration = GIDConfiguration(
            clientID: configuration.clientID,
            serverClientID: configuration.serverClientID
        )
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

    private func signIn(
        onIdToken: @escaping (String) -> Void,
        onError: @escaping (String) -> Void
    ) {
        guard pendingRequest == nil else { return }
        guard let window = activeWindow(), let presentingViewController = window.rootViewController else {
            onError("Não foi possível apresentar o início de sessão Google.")
            return
        }

        // GIDSignIn keeps only a weak reference to the presenter while it prepares
        // the OAuth request. Retain the window until the flow completes and present
        // from its stable root controller, rather than from a transient system auth
        // controller that may already be dismissing.
        pendingRequest = PendingRequest(
            window: window,
            onIdToken: onIdToken,
            onError: onError
        )
        GIDSignIn.sharedInstance.signIn(withPresenting: presentingViewController) { result, error in
            guard let request = self.takePendingRequest() else { return }

            if let error {
                #if DEBUG
                let nsError = error as NSError
                print(
                    "Google Sign-In failed: domain=\(nsError.domain) " +
                        "code=\(nsError.code) description=\(nsError.localizedDescription) " +
                        "userInfo=\(nsError.userInfo)"
                )
                #endif
                request.onError("Não foi possível iniciar sessão com Google. Tente novamente.")
                return
            }

            guard let idToken = result?.user.idToken?.tokenString, !idToken.isEmpty else {
                request.onError("Não foi possível obter a sessão Google. Tente novamente.")
                return
            }

            request.onIdToken(idToken)
        }
    }

    private func activeWindow() -> UIWindow? {
        UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .filter { $0.activationState == .foregroundActive }
            .flatMap(\.windows)
            .first { $0.isKeyWindow }
    }

    private func takePendingRequest() -> PendingRequest? {
        let request = pendingRequest
        pendingRequest = nil
        return request
    }

    private func logMissingConfiguration(_ issues: [String]) {
        #if DEBUG
        let details = issues.isEmpty ? "Unknown configuration error." : issues.joined(separator: " ")
        print("Google Sign-In disabled on iOS: \(details)")
        #endif
    }

    private struct PendingRequest {
        // The window is deliberately retained for the lifetime of the OAuth flow.
        let window: UIWindow
        let onIdToken: (String) -> Void
        let onError: (String) -> Void
    }
}

private struct GoogleSignInConfiguration {
    let clientID: String
    let serverClientID: String
    let reversedClientID: String

    static func load() -> (configuration: GoogleSignInConfiguration?, issues: [String]) {
        let googleServiceInfo = Bundle.main.googleServiceInfo
        let clientID = Bundle.main.nonPlaceholderInfoValue("GIDClientID")
            ?? googleServiceInfo.nonPlaceholderValue("CLIENT_ID")
        let serverClientID = Bundle.main.nonPlaceholderInfoValue("GIDServerClientID")
            ?? googleServiceInfo.nonPlaceholderValue("SERVER_CLIENT_ID")
        let reversedClientID = Bundle.main.nonPlaceholderInfoValue("GIDReversedClientID")
            ?? googleServiceInfo.nonPlaceholderValue("REVERSED_CLIENT_ID")

        var issues: [String] = []
        if clientID == nil {
            issues.append("Set GOOGLE_IOS_CLIENT_ID from GoogleService-Info.plist CLIENT_ID.")
        }
        if serverClientID == nil {
            issues.append("Set GOOGLE_WEB_CLIENT_ID from the Firebase web OAuth client.")
        }
        if reversedClientID == nil {
            issues.append("Set GOOGLE_IOS_REVERSED_CLIENT_ID from GoogleService-Info.plist REVERSED_CLIENT_ID.")
        }

        if let reversedClientID, !Bundle.main.registersURLScheme(reversedClientID) {
            issues.append("Register \(reversedClientID) in CFBundleURLTypes by setting GOOGLE_IOS_REVERSED_CLIENT_ID.")
        }

        guard
            issues.isEmpty,
            let clientID,
            let serverClientID,
            let reversedClientID
        else {
            return (nil, issues)
        }

        return (GoogleSignInConfiguration(
            clientID: clientID,
            serverClientID: serverClientID,
            reversedClientID: reversedClientID
        ), [])
    }
}

private extension Bundle {
    var googleServiceInfo: [String: Any] {
        guard
            let url = url(forResource: "GoogleService-Info", withExtension: "plist"),
            let plist = NSDictionary(contentsOf: url) as? [String: Any]
        else {
            return [:]
        }

        return plist
    }

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

    func registersURLScheme(_ scheme: String) -> Bool {
        guard let urlTypes = object(forInfoDictionaryKey: "CFBundleURLTypes") as? [[String: Any]] else {
            return false
        }

        return urlTypes
            .compactMap { $0["CFBundleURLSchemes"] as? [String] }
            .flatMap { $0 }
            .contains(scheme)
    }
}

private extension Dictionary where Key == String, Value == Any {
    func nonPlaceholderValue(_ key: String) -> String? {
        guard let value = self[key] as? String else {
            return nil
        }

        let trimmedValue = value.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmedValue.isEmpty, !trimmedValue.contains("$(") else {
            return nil
        }

        return trimmedValue
    }
}

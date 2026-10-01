import UIKit
import UserNotifications
import FirebaseCore
import FirebaseMessaging
import FirebaseInstallations
import ComposeApp

final class SudsAppDelegate: NSObject, UIApplicationDelegate {
    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil) -> Bool {
        SudsPushNotifications.shared.start()
        return true
    }

    func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        SudsPushNotifications.shared.didRegisterAPNs(deviceToken)
    }

    func application(_ application: UIApplication, didFailToRegisterForRemoteNotificationsWithError error: Error) {
        SudsPushNotifications.shared.didFailAPNs()
    }
}

final class SudsPushNotifications: NSObject, UNUserNotificationCenterDelegate, MessagingDelegate {
    static let shared = SudsPushNotifications()
    private var waitingForAPNs: [((String) -> Void, (String) -> Void)] = []
    private var lastInstallationId: String?

    func configureKotlinBridge() {
        IosNotificationsKt.configureIosNotificationRegistration { [weak self] onId, onError in
            DispatchQueue.main.async {
                self?.register(onId: { id in _ = onId(id) }, onError: { error in _ = onError(error) })
            }
        }
    }

    func start() {
        if FirebaseApp.app() == nil { FirebaseApp.configure() }
        UNUserNotificationCenter.current().delegate = self
        Messaging.messaging().delegate = self
        UNUserNotificationCenter.current().getNotificationSettings { settings in
            // Request once so iOS exposes the app's notification controls in Settings.
            if settings.authorizationStatus == .notDetermined {
                UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .badge, .sound]) { _, _ in
                    DispatchQueue.main.async { IosNotificationsKt.refreshIosNotificationRegistration() }
                }
            }
        }
    }

    private func register(onId: @escaping (String) -> Void, onError: @escaping (String) -> Void) {
        guard FirebaseApp.app() != nil else {
            onError("Não foi possível preparar as notificações.")
            return
        }
        if Messaging.messaging().apnsToken == nil {
            waitingForAPNs.append((onId, onError))
            UIApplication.shared.registerForRemoteNotifications()
            return
        }
        Messaging.messaging().register { error in
            guard error == nil else {
                onError("Não foi possível registar notificações.")
                return
            }
            Installations.installations().installationID { id, error in
                DispatchQueue.main.async {
                    if error == nil, let id, !id.isEmpty { onId(id) }
                    else { onError("Não foi possível obter o registo deste iPhone.") }
                }
            }
        }
    }

    func didRegisterAPNs(_ token: Data) {
        Messaging.messaging().apnsToken = token
        let waiting = waitingForAPNs
        waitingForAPNs.removeAll()
        waiting.forEach { register(onId: $0.0, onError: $0.1) }
    }

    func didFailAPNs() {
        let waiting = waitingForAPNs
        waitingForAPNs.removeAll()
        waiting.forEach { $0.1("Não foi possível ligar às notificações da Apple.") }
    }

    func messaging(_ messaging: Messaging, didReceiveRegistration installationId: String?) {
        guard let installationId, installationId != lastInstallationId else { return }
        lastInstallationId = installationId
        DispatchQueue.main.async { IosNotificationsKt.refreshIosNotificationRegistration() }
    }

    func userNotificationCenter(_ center: UNUserNotificationCenter, willPresent notification: UNNotification, withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void) {
        completionHandler([.banner, .list, .sound])
    }

    func userNotificationCenter(_ center: UNUserNotificationCenter, didReceive response: UNNotificationResponse, withCompletionHandler completionHandler: @escaping () -> Void) {
        let data = response.notification.request.content.userInfo.reduce(into: [String: String]()) { result, item in
            if let key = item.key as? String, let value = item.value as? String { result[key] = value }
        }
        DispatchQueue.main.async {
            IosNotificationsKt.openIosPushNotification(data: data)
            completionHandler()
        }
    }
}

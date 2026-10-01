import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(SudsAppDelegate.self) var appDelegate
    init() {
        #if DEBUG
        let isDebugBuild = true
        #else
        let isDebugBuild = false
        #endif
        ComposeApp.KoinInitializerKt.initializeIosApp(isDebugBuild: isDebugBuild)
        GoogleSignInCoordinator.shared.configureKotlinBridge()
        AppleSignInCoordinator.shared.configureKotlinBridge()
        AppleAccountDeletionCoordinator.shared.configureKotlinBridge()
        SudsPushNotifications.shared.configureKotlinBridge()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL { url in
                    _ = GoogleSignInCoordinator.shared.handleOpenURL(url)
                }
        }
    }
}

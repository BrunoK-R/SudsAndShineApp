import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    init() {
        #if DEBUG
        let isDebugBuild = true
        #else
        let isDebugBuild = false
        #endif
        ComposeApp.KoinInitializerKt.initializeIosApp(isDebugBuild: isDebugBuild)
        GoogleSignInCoordinator.shared.configureKotlinBridge()
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

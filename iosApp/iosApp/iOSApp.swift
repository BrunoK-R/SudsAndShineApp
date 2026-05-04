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
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}

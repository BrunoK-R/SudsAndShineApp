import SwiftUI
import UIKit
import ComposeApp
import CoreMotion

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    @Environment(\.scenePhase) private var scenePhase
    @StateObject private var shakeDetector = SudsShakeDetector()

    var body: some View {
        ComposeView()
            .ignoresSafeArea(edges: .all)
            .onAppear { shakeDetector.start() }
            .onDisappear { shakeDetector.stop() }
            .onChange(of: scenePhase) { _, phase in
                if phase == .active { shakeDetector.start() } else { shakeDetector.stop() }
            }
    }
}

private final class SudsShakeDetector: ObservableObject {
    private let motionManager = CMMotionManager()
    private let queue = OperationQueue()
    private var impulses: [TimeInterval] = []
    private var aboveThreshold = false
    private var lastShakeAt: TimeInterval = -.infinity

    init() {
        queue.maxConcurrentOperationCount = 1
    }

    func start() {
        guard motionManager.isAccelerometerAvailable, !motionManager.isAccelerometerActive else { return }
        motionManager.accelerometerUpdateInterval = 1.0 / 50.0
        motionManager.startAccelerometerUpdates(to: queue) { [weak self] sample, _ in
            guard let self, let sample else { return }
            let acceleration = sample.acceleration
            let magnitude = sqrt(acceleration.x * acceleration.x +
                                 acceleration.y * acceleration.y + acceleration.z * acceleration.z)
            if magnitude < 2.7 {
                self.aboveThreshold = false
                return
            }
            guard !self.aboveThreshold else { return }
            self.aboveThreshold = true
            let now = sample.timestamp
            guard now - self.lastShakeAt >= 1.5 else { return }
            self.impulses.removeAll { now - $0 > 0.5 }
            self.impulses.append(now)
            guard self.impulses.count >= 2 else { return }
            self.impulses.removeAll()
            self.lastShakeAt = now
            DispatchQueue.main.async {
                let capture = Self.captureScreen()
                ShakeFeedbackKt.requestShakeFeedbackFromIos(
                    screenshotBase64: capture?.data.base64EncodedString(),
                    widthPx: Int32(capture?.width ?? 0),
                    heightPx: Int32(capture?.height ?? 0)
                )
            }
        }
    }

    func stop() {
        motionManager.stopAccelerometerUpdates()
    }

    private static func captureScreen() -> (data: Data, width: Int, height: Int)? {
        guard let window = UIApplication.shared.connectedScenes
            .compactMap({ $0 as? UIWindowScene })
            .flatMap(\.windows)
            .first(where: { $0.isKeyWindow }) else { return nil }
        let renderer = UIGraphicsImageRenderer(bounds: window.bounds)
        var didDraw = false
        let image = renderer.image { _ in
            didDraw = window.drawHierarchy(in: window.bounds, afterScreenUpdates: false)
        }
        guard didDraw, let data = image.jpegData(compressionQuality: 0.7),
              data.count <= 5 * 1024 * 1024, let cgImage = image.cgImage else { return nil }
        return (data, cgImage.width, cgImage.height)
    }
}

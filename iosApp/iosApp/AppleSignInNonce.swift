import CryptoKit
import Foundation
import Security

enum AppleSignInNonce {
    static func generate() throws -> String {
        var bytes = [UInt8](repeating: 0, count: 32)
        let status = SecRandomCopyBytes(kSecRandomDefault, bytes.count, &bytes)
        guard status == errSecSuccess else {
            throw NSError(domain: NSOSStatusErrorDomain, code: Int(status))
        }
        // Hex encoding preserves all 256 random bits without modulo bias.
        return bytes.map { String(format: "%02x", $0) }.joined()
    }

    static func sha256(_ nonce: String) -> String {
        SHA256.hash(data: Data(nonce.utf8))
            .map { String(format: "%02x", $0) }
            .joined()
    }
}

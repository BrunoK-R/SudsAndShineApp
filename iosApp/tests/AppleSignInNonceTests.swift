import Foundation

// Runs on macOS without signing, an Apple account, or an iOS test host.
@main
enum AppleSignInNonceTests {
    static func main() throws {
        precondition(AppleSignInNonce.sha256("abc") ==
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad")
        precondition(AppleSignInNonce.sha256("") ==
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")
        let nonces = try (0..<100).map { _ in try AppleSignInNonce.generate() }
        precondition(Set(nonces).count == nonces.count)
        precondition(nonces.allSatisfy { nonce in
            nonce.count == 64 && nonce.allSatisfy { "0123456789abcdef".contains($0) }
                && AppleSignInNonce.sha256(nonce).count == 64
        })
        print("Apple nonce tests passed (SHA-256 vectors, encoding, independent random requests).")
    }
}

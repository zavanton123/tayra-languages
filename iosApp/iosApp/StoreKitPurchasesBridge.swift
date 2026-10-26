import Foundation
import StoreKit
import Shared

/// Apple's StoreKit 2, handed to the shared code as `StoreKitBridge`. A purchase carries the account's
/// id as its appAccountToken, and its signed transaction goes to the account service.
final class StoreKitPurchasesBridge: NSObject, StoreKitBridge {
    private var updates: Task<Void, Never>?

    override init() {
        super.init()
        // Transactions that end outside a purchase here (renewals, Ask to Buy, another device) are
        // finished as they come; the account service hears of them from the App Store itself.
        updates = Task.detached {
            for await result in Transaction.updates {
                if case .verified(let transaction) = result { await transaction.finish() }
            }
        }
    }

    deinit { updates?.cancel() }

    func products(ids: [String], callback: @escaping ([StoreKitProduct]?, String?) -> Void) {
        Task {
            do {
                let products = try await Product.products(for: ids)
                callback(products.map { StoreKitProduct(id: $0.id, title: $0.displayName, price: $0.displayPrice) }, nil)
            } catch {
                callback(nil, error.localizedDescription)
            }
        }
    }

    func buy(productId: String, accountToken: String, callback: @escaping (String?, String?) -> Void) {
        Task {
            do {
                guard let product = try await Product.products(for: [productId]).first else {
                    return callback(nil, "The App Store does not sell \(productId) here")
                }
                guard let token = UUID(uuidString: accountToken) else { return callback(nil, "No account to buy for") }
                switch try await product.purchase(options: [.appAccountToken(token)]) {
                case .success(let result):
                    guard case .verified(let transaction) = result else { return callback(nil, "The App Store could not verify the purchase") }
                    await transaction.finish()
                    callback(result.jwsRepresentation, nil)
                case .userCancelled:
                    callback(nil, nil)
                case .pending:
                    callback(nil, "The purchase waits for approval; the subscription starts once it is approved")
                @unknown default:
                    callback(nil, "The App Store gave an answer this version does not know")
                }
            } catch {
                callback(nil, error.localizedDescription)
            }
        }
    }

    func owned(callback: @escaping ([String]?, String?) -> Void) {
        Task {
            // Restoring asks the App Store for the account's purchases first, as Apple's guidelines expect.
            try? await AppStore.sync()
            var signed: [String] = []
            for await result in Transaction.currentEntitlements {
                if case .verified = result { signed.append(result.jwsRepresentation) }
            }
            callback(signed, nil)
        }
    }
}

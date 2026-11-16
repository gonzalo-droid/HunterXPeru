import AppTrackingTransparency
import ComposeApp
import GoogleMobileAds
import UIKit
import UserMessagingPlatform

/// Implementa `IosAdsBridge` (Kotlin). Orden obligatorio: UMP → ATT → MobileAds.start.
/// El producto SPM se llama GoogleUserMessagingPlatform, pero el módulo es UserMessagingPlatform.
final class AdsBridge: NSObject, IosAdsBridge {

    private var bannerUnitId: String {
        Bundle.main.object(forInfoDictionaryKey: "CZAdmobBannerId") as? String
            ?? "ca-app-pub-3940256099942544/2435281174"
    }

    func requestConsentAndStart(debugGeographyEea: Bool, completion: @escaping (AdsConsentState) -> Void) {
        let parameters = RequestParameters()
        if debugGeographyEea {
            // Solo debug: fuerza el formulario aunque el simulador esté en Perú.
            let debugSettings = DebugSettings()
            debugSettings.geography = .EEA
            parameters.debugSettings = debugSettings
        }
        ConsentInformation.shared.requestConsentInfoUpdate(with: parameters) { [weak self] _ in
            DispatchQueue.main.async {
                ConsentForm.loadAndPresentIfRequired(from: Self.topViewController()) { _ in
                    self?.requestTrackingThenStart(completion: completion)
                }
            }
        }
    }

    func showPrivacyOptions(completion: @escaping (AdsConsentState) -> Void) {
        ConsentForm.presentPrivacyOptionsForm(from: Self.topViewController()) { [weak self] _ in
            completion(self?.currentState(sdkStarted: true) ?? AdsConsentState(canRequestAds: false, privacyOptionsRequired: true))
        }
    }

    func makeBannerView(widthPoints: Double) -> UIView {
        let banner = BannerView(adSize: currentOrientationAnchoredAdaptiveBanner(width: CGFloat(widthPoints)))
        banner.adUnitID = bannerUnitId
        banner.rootViewController = Self.topViewController()
        banner.load(Request())
        return banner
    }

    func bannerHeight(widthPoints: Double) -> Double {
        Double(currentOrientationAnchoredAdaptiveBanner(width: CGFloat(widthPoints)).size.height)
    }

    // MARK: - Privado

    private func requestTrackingThenStart(completion: @escaping (AdsConsentState) -> Void) {
        ATTrackingManager.requestTrackingAuthorization { [weak self] _ in
            DispatchQueue.main.async {
                guard let self else { return }
                guard ConsentInformation.shared.canRequestAds else {
                    completion(self.currentState(sdkStarted: false))
                    return
                }
                MobileAds.shared.start { _ in
                    DispatchQueue.main.async { completion(self.currentState(sdkStarted: true)) }
                }
            }
        }
    }

    private func currentState(sdkStarted: Bool) -> AdsConsentState {
        AdsConsentState(
            canRequestAds: sdkStarted && ConsentInformation.shared.canRequestAds,
            privacyOptionsRequired: ConsentInformation.shared.privacyOptionsRequirementStatus == .required
        )
    }

    private static func topViewController() -> UIViewController? {
        let window = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap(\.windows)
            .first(where: \.isKeyWindow)
        var top = window?.rootViewController
        while let presented = top?.presentedViewController { top = presented }
        return top
    }
}

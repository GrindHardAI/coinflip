package com.coinflipper.app.monetization

import android.app.Activity
import android.os.SystemClock
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.coinflipper.app.BuildConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

/** Everything the UI needs from ads and in-app purchases. */
interface Monetization {
    /** True once consent is settled and the ads SDK is ready. */
    val adsReady: StateFlow<Boolean>
    val rewardedReady: StateFlow<Boolean>
    /** EEA/UK users must be able to reopen the consent form from settings. */
    val privacyOptionsRequired: StateFlow<Boolean>
    /** Localized price of "Remove ads", or null when the product isn't available. */
    val removeAdsPrice: StateFlow<String?>

    fun onFlipCompleted(adsRemoved: Boolean)
    /** Shows a rewarded video; [onReward] runs only if the user watched it. False if none is ready. */
    fun showRewarded(onReward: () -> Unit): Boolean
    fun buyRemoveAds(): Boolean
    fun showPrivacyOptions()
}

/** Used for previews and screenshot runs: no ads, no billing. */
object NoMonetization : Monetization {
    override val adsReady = MutableStateFlow(false).asStateFlow()
    override val rewardedReady = MutableStateFlow(false).asStateFlow()
    override val privacyOptionsRequired = MutableStateFlow(false).asStateFlow()
    override val removeAdsPrice = MutableStateFlow<String?>(null).asStateFlow()
    override fun onFlipCompleted(adsRemoved: Boolean) = Unit
    override fun showRewarded(onReward: () -> Unit) = false
    override fun buyRemoveAds() = false
    override fun showPrivacyOptions() = Unit
}

/**
 * Google AdMob (with the UMP consent form) + Google Play Billing for the
 * one-time "remove_ads" product.
 */
class PlayMonetization(
    private val activity: Activity,
    private val onAdsRemovedChanged: (Boolean) -> Unit,
) : Monetization, PurchasesUpdatedListener {

    private val consent: ConsentInformation = UserMessagingPlatform.getConsentInformation(activity)
    private val sdkStarted = AtomicBoolean(false)
    private val pacing = AdPacing(sessionStartMs = SystemClock.elapsedRealtime())
    private var interstitial: InterstitialAd? = null
    private var rewarded: RewardedAd? = null
    private var removeAdsDetails: ProductDetails? = null

    private val _adsReady = MutableStateFlow(false)
    private val _rewardedReady = MutableStateFlow(false)
    private val _privacyOptionsRequired = MutableStateFlow(false)
    private val _removeAdsPrice = MutableStateFlow<String?>(null)
    override val adsReady = _adsReady.asStateFlow()
    override val rewardedReady = _rewardedReady.asStateFlow()
    override val privacyOptionsRequired = _privacyOptionsRequired.asStateFlow()
    override val removeAdsPrice = _removeAdsPrice.asStateFlow()

    private val billing = BillingClient.newBuilder(activity)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    fun start() {
        startConsentAndAds()
        startBilling()
    }

    fun stop() {
        billing.endConnection()
    }

    // ---------- Consent + ads ----------

    private fun startConsentAndAds() {
        val params = ConsentRequestParameters.Builder().build()
        consent.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { error ->
                    if (error != null) Log.w(TAG, "Consent form: ${error.message}")
                    _privacyOptionsRequired.value = consent.privacyOptionsRequirementStatus ==
                        ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
                    initAdsIfAllowed()
                }
            },
            { error ->
                Log.w(TAG, "Consent info: ${error.message}")
                initAdsIfAllowed()
            },
        )
        // Consent given in an earlier session lets ads start right away.
        initAdsIfAllowed()
    }

    private fun initAdsIfAllowed() {
        if (!consent.canRequestAds() || !sdkStarted.compareAndSet(false, true)) return
        // Initialize off the main thread, as Google recommends.
        Thread {
            MobileAds.initialize(activity) {
                activity.runOnUiThread {
                    _adsReady.value = true
                    loadInterstitial()
                    loadRewarded()
                }
            }
        }.start()
    }

    override fun showPrivacyOptions() {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { error ->
            if (error != null) Log.w(TAG, "Privacy options: ${error.message}")
        }
    }

    private fun loadInterstitial() {
        InterstitialAd.load(
            activity,
            BuildConfig.ADMOB_INTERSTITIAL_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitial = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitial = null
                    Log.w(TAG, "Interstitial failed: ${error.message}")
                }
            },
        )
    }

    private fun loadRewarded() {
        RewardedAd.load(
            activity,
            BuildConfig.ADMOB_REWARDED_ID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewarded = ad
                    _rewardedReady.value = true
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewarded = null
                    _rewardedReady.value = false
                    Log.w(TAG, "Rewarded failed: ${error.message}")
                }
            },
        )
    }

    override fun onFlipCompleted(adsRemoved: Boolean) {
        if (!_adsReady.value || adsRemoved) return
        pacing.recordFlip()
        val now = SystemClock.elapsedRealtime()
        val ad = interstitial ?: return
        if (!pacing.shouldShow(now)) return
        ad.fullScreenContentCallback = reloadAfter { loadInterstitial() }
        interstitial = null
        pacing.markShown(now)
        ad.show(activity)
    }

    override fun showRewarded(onReward: () -> Unit): Boolean {
        val ad = rewarded
        if (ad == null) {
            if (_adsReady.value) loadRewarded()
            return false
        }
        rewarded = null
        _rewardedReady.value = false
        ad.fullScreenContentCallback = reloadAfter { loadRewarded() }
        ad.show(activity) { onReward() }
        return true
    }

    private fun reloadAfter(reload: () -> Unit) = object : FullScreenContentCallback() {
        override fun onAdDismissedFullScreenContent() = reload()
        override fun onAdFailedToShowFullScreenContent(error: AdError) = reload()
    }

    // ---------- Billing: "Remove ads" ----------

    private fun startBilling() {
        billing.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingResponseCode.OK) {
                    queryRemoveAdsProduct()
                    restorePurchases()
                } else {
                    Log.w(TAG, "Billing setup: ${result.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() = Unit
        })
    }

    private fun queryRemoveAdsProduct() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(REMOVE_ADS_PRODUCT_ID)
                        .setProductType(ProductType.INAPP)
                        .build(),
                ),
            )
            .build()
        billing.queryProductDetailsAsync(params) { result, details ->
            if (result.responseCode != BillingResponseCode.OK) return@queryProductDetailsAsync
            val product = details.productDetailsList.firstOrNull { it.productId == REMOVE_ADS_PRODUCT_ID }
            removeAdsDetails = product
            _removeAdsPrice.value = product?.oneTimePurchaseOfferDetails?.formattedPrice
        }
    }

    private fun restorePurchases() {
        billing.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(ProductType.INAPP).build(),
        ) { result, purchases ->
            if (result.responseCode != BillingResponseCode.OK) return@queryPurchasesAsync
            val owned = purchases.filter(::isRemoveAds)
            owned.forEach(::acknowledgeIfNeeded)
            onAdsRemovedChanged(owned.isNotEmpty())
        }
    }

    override fun buyRemoveAds(): Boolean {
        val product = removeAdsDetails ?: return false
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(product).build()),
            )
            .build()
        return billing.launchBillingFlow(activity, params).responseCode == BillingResponseCode.OK
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingResponseCode.OK -> purchases.orEmpty().filter(::isRemoveAds).forEach {
                acknowledgeIfNeeded(it)
                onAdsRemovedChanged(true)
            }
            BillingResponseCode.ITEM_ALREADY_OWNED -> restorePurchases()
            else -> Log.i(TAG, "Purchase not completed: ${result.responseCode}")
        }
    }

    private fun isRemoveAds(p: Purchase) =
        REMOVE_ADS_PRODUCT_ID in p.products && p.purchaseState == Purchase.PurchaseState.PURCHASED

    private fun acknowledgeIfNeeded(p: Purchase) {
        if (p.isAcknowledged) return
        val params = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(p.purchaseToken).build()
        billing.acknowledgePurchase(params) { r ->
            if (r.responseCode != BillingResponseCode.OK) Log.w(TAG, "Acknowledge failed: ${r.debugMessage}")
        }
    }

    companion object {
        private const val TAG = "Monetization"
        /** Create this one-time product in Play Console → Monetize → In-app products. */
        const val REMOVE_ADS_PRODUCT_ID = "remove_ads"
    }
}

package com.example.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

object AdMobManager {
    private const val TAG = "AdMobManager"

    // Official AdMob Unit IDs configured by user
    const val APP_ID = "ca-app-pub-4408731854837351~2646131552"
    const val BANNER_AD_ID = "ca-app-pub-4408731854837351/6397674304"
    const val INTERSTITIAL_AD_ID = "ca-app-pub-4408731854837351/4809260468"
    const val REWARDED_INTERSTITIAL_AD_ID = "ca-app-pub-4408731854837351/2167514204"
    const val NATIVE_ADVANCE_AD_ID = "ca-app-pub-4408731854837351/9854432534"
    const val APP_OPEN_AD_ID = "ca-app-pub-4408731854837351/6737314608"

    private val isInitialized = AtomicBoolean(false)

    private var interstitialAd: InterstitialAd? = null
    private var isInterstitialLoading = false

    private var rewardedInterstitialAd: RewardedInterstitialAd? = null
    private var isRewardedInterstitialLoading = false

    private var appOpenAd: AppOpenAd? = null
    private var isAppOpenLoading = false
    private var isShowingAppOpenAd = false

    fun initialize(context: Context) {
        if (isInitialized.getAndSet(true)) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                MobileAds.initialize(context) { status ->
                    Log.d(TAG, "MobileAds initialized: $status")
                    // Preload initial ads
                    loadInterstitialAd(context)
                    loadRewardedInterstitialAd(context)
                    loadAppOpenAd(context)
                }
            } catch (e: Exception) {
                Log.w(TAG, "AdMob initialization warning: ${e.message}")
            }
        }
    }

    /**
     * Preloads an Interstitial Ad
     */
    fun loadInterstitialAd(context: Context) {
        if (interstitialAd != null || isInterstitialLoading) return
        isInterstitialLoading = true

        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            INTERSTITIAL_AD_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isInterstitialLoading = false
                    Log.d(TAG, "InterstitialAd loaded successfully.")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    interstitialAd = null
                    isInterstitialLoading = false
                    Log.w(TAG, "InterstitialAd failed to load: ${loadAdError.message}")
                }
            }
        )
    }

    /**
     * Shows an Interstitial Ad if ready, or invokes onAdDismissed immediately.
     */
    fun showInterstitialAd(activity: Activity, onAdDismissed: () -> Unit = {}) {
        val ad = interstitialAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    loadInterstitialAd(activity)
                    onAdDismissed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    interstitialAd = null
                    loadInterstitialAd(activity)
                    onAdDismissed()
                }
            }
            ad.show(activity)
        } else {
            // Not ready yet, proceed smoothly
            loadInterstitialAd(activity)
            onAdDismissed()
        }
    }

    /**
     * Preloads a Rewarded Interstitial Ad
     */
    fun loadRewardedInterstitialAd(context: Context) {
        if (rewardedInterstitialAd != null || isRewardedInterstitialLoading) return
        isRewardedInterstitialLoading = true

        val adRequest = AdRequest.Builder().build()
        RewardedInterstitialAd.load(
            context,
            REWARDED_INTERSTITIAL_AD_ID,
            adRequest,
            object : RewardedInterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedInterstitialAd) {
                    rewardedInterstitialAd = ad
                    isRewardedInterstitialLoading = false
                    Log.d(TAG, "RewardedInterstitialAd loaded successfully.")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    rewardedInterstitialAd = null
                    isRewardedInterstitialLoading = false
                    Log.w(TAG, "RewardedInterstitialAd failed to load: ${loadAdError.message}")
                }
            }
        )
    }

    /**
     * Shows a Rewarded Interstitial Ad to the user
     */
    fun showRewardedInterstitialAd(
        activity: Activity,
        onUserEarnedReward: () -> Unit,
        onAdDismissed: () -> Unit = {}
    ) {
        val ad = rewardedInterstitialAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    rewardedInterstitialAd = null
                    loadRewardedInterstitialAd(activity)
                    onAdDismissed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    rewardedInterstitialAd = null
                    loadRewardedInterstitialAd(activity)
                    onAdDismissed()
                }
            }
            ad.show(activity) { _ ->
                onUserEarnedReward()
            }
        } else {
            loadRewardedInterstitialAd(activity)
            onAdDismissed()
        }
    }

    /**
     * Preloads App Open Ad
     */
    fun loadAppOpenAd(context: Context) {
        if (appOpenAd != null || isAppOpenLoading) return
        isAppOpenLoading = true

        val adRequest = AdRequest.Builder().build()
        AppOpenAd.load(
            context,
            APP_OPEN_AD_ID,
            adRequest,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    isAppOpenLoading = false
                    Log.d(TAG, "AppOpenAd loaded successfully.")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    appOpenAd = null
                    isAppOpenLoading = false
                    Log.w(TAG, "AppOpenAd failed to load: ${loadAdError.message}")
                }
            }
        )
    }

    /**
     * Shows App Open Ad if available
     */
    fun showAppOpenAdIfAvailable(activity: Activity, onAdDismissed: () -> Unit = {}) {
        val ad = appOpenAd
        if (ad != null && !isShowingAppOpenAd) {
            isShowingAppOpenAd = true
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    appOpenAd = null
                    isShowingAppOpenAd = false
                    loadAppOpenAd(activity)
                    onAdDismissed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    appOpenAd = null
                    isShowingAppOpenAd = false
                    loadAppOpenAd(activity)
                    onAdDismissed()
                }
            }
            ad.show(activity)
        } else {
            loadAppOpenAd(activity)
            onAdDismissed()
        }
    }
}

/**
 * Composable Banner Ad View embedding Google Mobile Ads SDK AdView
 */
@Composable
fun AdMobBanner(
    adUnitId: String = AdMobManager.BANNER_AD_ID,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { context ->
                AdView(context).apply {
                    setAdSize(AdSize.BANNER)
                    this.adUnitId = adUnitId
                    try {
                        loadAd(AdRequest.Builder().build())
                    } catch (e: Exception) {
                        Log.w("AdMobBanner", "Failed to load banner ad: ${e.message}")
                    }
                }
            },
            update = { adView ->
                // Banner view persists
            }
        )
    }
}

package com.example.mindfocus.core.auth

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class BiometricAuthManager(private val context: Context) {
    
    fun isBiometricAvailable(): Boolean {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            else -> false
        }
    }
    
    suspend fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        negativeButtonText: String
    ): BiometricResult = suspendCancellableCoroutine { continuation ->
        val fragmentManager = activity.supportFragmentManager
        val fragmentTag = context.getString(com.example.mindfocus.R.string.biometric_fragment_tag)
        
        val existingFragment = fragmentManager.findFragmentByTag(fragmentTag) as? BiometricFragment
        if (existingFragment != null) {
            try {
                fragmentManager.beginTransaction()
                    .remove(existingFragment)
                    .commitNow()
            } catch (_: Exception) {
            }
        }
        
        val fragment = BiometricFragment()
        fragment.setAuthParams(title, subtitle, negativeButtonText) { result ->
            continuation.resume(result)
            try {
                if (fragment.isAdded) {
                    fragmentManager.beginTransaction()
                        .remove(fragment)
                        .commitAllowingStateLoss()
                }
            } catch (_: Exception) {
            }
        }
        
        try {
            fragmentManager.beginTransaction()
                .add(fragment, fragmentTag)
                .commitNow()
        } catch (_: Exception) {
            fragmentManager.beginTransaction()
                .add(fragment, fragmentTag)
                .commitAllowingStateLoss()
            fragmentManager.executePendingTransactions()
        }
        
        continuation.invokeOnCancellation {
            try {
                val frag = fragmentManager.findFragmentByTag(fragmentTag) as? BiometricFragment
                if (frag != null && frag.isAdded) {
                    fragmentManager.beginTransaction()
                        .remove(frag)
                        .commitAllowingStateLoss()
                }
            } catch (_: Exception) {
            }
        }
    }
}

sealed class BiometricResult {
    object Success : BiometricResult()
    data class Error(val message: String) : BiometricResult()
    object Failed : BiometricResult()
}


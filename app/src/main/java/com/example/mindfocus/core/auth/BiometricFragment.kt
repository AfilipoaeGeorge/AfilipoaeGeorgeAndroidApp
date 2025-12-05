package com.example.mindfocus.core.auth

import android.os.Bundle
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner

class BiometricFragment : Fragment() {
    
    private var onResult: ((BiometricResult) -> Unit)? = null
    private var title: String? = null
    private var subtitle: String? = null
    private var negativeButtonText: String? = null
    private var hasAuthenticated = false
    
    fun setAuthParams(
        title: String,
        subtitle: String,
        negativeButtonText: String,
        callback: (BiometricResult) -> Unit
    ) {
        this.title = title
        this.subtitle = subtitle
        this.negativeButtonText = negativeButtonText
        this.onResult = callback
        this.hasAuthenticated = false
        
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            authenticate()
        }
    }
    
    override fun onStart() {
        super.onStart()
        view?.post {
            if (!hasAuthenticated && title != null && subtitle != null && negativeButtonText != null) {
                authenticate()
            }
        } ?: run {
            if (!hasAuthenticated && title != null && subtitle != null && negativeButtonText != null) {
                authenticate()
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        if (!hasAuthenticated && title != null && subtitle != null && negativeButtonText != null) {
            authenticate()
        }
    }
    
    private fun authenticate() {
        if (hasAuthenticated) return
        
        val title = this.title ?: return
        val subtitle = this.subtitle ?: return
        val negativeButtonText = this.negativeButtonText ?: return
        val callback = this.onResult ?: return
        
        if (!isAdded || context == null) return
        
        hasAuthenticated = true
        
        val executor = ContextCompat.getMainExecutor(requireContext())
        val biometricPrompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    callback.invoke(BiometricResult.Success)
                    cleanup()
                }
                
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    callback.invoke(BiometricResult.Error(errString.toString()))
                    cleanup()
                }
                
                override fun onAuthenticationFailed() {
                    callback.invoke(BiometricResult.Failed)
                    cleanup()
                }
            }
        )
        
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negativeButtonText)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()
        
        biometricPrompt.authenticate(promptInfo)
    }
    
    private fun cleanup() {
        onResult = null
        title = null
        subtitle = null
        negativeButtonText = null
    }
    
    override fun onDestroy() {
        super.onDestroy()
        cleanup()
    }
}


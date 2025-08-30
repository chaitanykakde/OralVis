package com.nextserve.oralvishealth.ui.splash

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.nextserve.oralvishealth.MainActivity
import com.nextserve.oralvishealth.databinding.ActivitySplashBinding
import com.nextserve.oralvishealth.ui.auth.LoginActivity

class SplashActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySplashBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        startLogoAnimation()
        
        Handler(Looper.getMainLooper()).postDelayed({
            checkAuthenticationAndNavigate()
        }, 2500)
    }

    private fun startLogoAnimation() {
        val scaleX = ObjectAnimator.ofFloat(binding.ivLogo, "scaleX", 0f, 1f)
        val scaleY = ObjectAnimator.ofFloat(binding.ivLogo, "scaleY", 0f, 1f)
        val alpha = ObjectAnimator.ofFloat(binding.ivLogo, "alpha", 0f, 1f)

        val animatorSet = AnimatorSet()
        animatorSet.playTogether(scaleX, scaleY, alpha)
        animatorSet.duration = 1000
        animatorSet.start()
    }

    private fun checkAuthenticationAndNavigate() {
        val currentUser = auth.currentUser
        val intent = if (currentUser != null) {
            Intent(this, MainActivity::class.java)
        } else {
            Intent(this, LoginActivity::class.java)
        }
        startActivity(intent)
        finish()
    }
}

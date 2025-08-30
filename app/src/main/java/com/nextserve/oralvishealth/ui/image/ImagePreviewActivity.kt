package com.nextserve.oralvishealth.ui.image

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.nextserve.oralvishealth.databinding.ActivityImagePreviewBinding
import java.io.File

class ImagePreviewActivity : AppCompatActivity() {
    private lateinit var binding: ActivityImagePreviewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImagePreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val imagePath = intent.getStringExtra("imagePath")
        if (imagePath != null) {
            loadImage(imagePath)
        } else {
            finish()
        }

        setupClickListeners()
    }

    private fun loadImage(imagePath: String) {
        val imageFile = File(imagePath)
        if (imageFile.exists()) {
            Glide.with(this)
                .load(imageFile)
                .into(binding.ivPreview)
        }
    }

    private fun setupClickListeners() {
        binding.btnClose.setOnClickListener {
            finish()
        }

        binding.ivPreview.setOnClickListener {
            // Toggle UI visibility on image tap
            toggleUIVisibility()
        }
    }

    private fun toggleUIVisibility() {
        val isVisible = binding.btnClose.alpha == 1f
        val targetAlpha = if (isVisible) 0f else 1f
        
        binding.btnClose.animate()
            .alpha(targetAlpha)
            .setDuration(300)
            .start()
    }
}

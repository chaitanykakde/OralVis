package com.nextserve.oralvishealth.ui.session

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.nextserve.oralvishealth.R
import com.nextserve.oralvishealth.databinding.ActivitySessionDetailsBinding
import com.nextserve.oralvishealth.ui.adapter.ImageAdapter
import com.nextserve.oralvishealth.ui.adapter.ShimmerImageAdapter
import com.nextserve.oralvishealth.ui.viewmodel.SessionViewModel
import com.nextserve.oralvishealth.service.PDFReportService
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import android.view.View
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class SessionDetailsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySessionDetailsBinding
    private lateinit var sessionViewModel: SessionViewModel
    private lateinit var imageAdapter: ImageAdapter
    private var currentSessionId: String? = null
    private var currentImageFiles: List<File> = emptyList()
    private var generatedPdfFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySessionDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionViewModel = ViewModelProvider(this)[SessionViewModel::class.java]
        
        setupToolbar()
        setupReportGeneration()
        
        val sessionId = intent.getStringExtra("sessionId")
        if (sessionId != null) {
            currentSessionId = sessionId
            loadSessionDetails(sessionId)
            loadSessionImages(sessionId)
        } else {
            finish()
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun loadSessionDetails(sessionId: String) {
        lifecycleScope.launch {
            val session = sessionViewModel.getSessionById(sessionId)
            session?.let {
                binding.tvSessionIdLabel.text = it.sessionId
                binding.tvNameLabel.text = it.name
                binding.tvAgeLabel.text = "${it.age} years"
                
                // Format timestamp
                val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                val date = Date(it.timestamp)
                
                binding.tvTimestamp.text = dateFormat.format(date)
                binding.tvTime.text = timeFormat.format(date)
            }
        }
    }

    private fun loadSessionImages(sessionId: String) {
        val sessionDir = File(getExternalFilesDir(null), "Sessions/$sessionId")
        
        // First check if images exist locally
        if (sessionDir.exists() && sessionDir.isDirectory) {
            val imageFiles = sessionDir.listFiles { file ->
                file.isFile && (file.extension.lowercase() == "jpg" || file.extension.lowercase() == "jpeg" || file.extension.lowercase() == "png")
            }?.toList() ?: emptyList()
            
            if (imageFiles.isNotEmpty()) {
                currentImageFiles = imageFiles  // Fix: Update currentImageFiles for PDF generation
                setupImageRecyclerView(imageFiles)
                binding.rvImages.visibility = View.VISIBLE
                binding.tvNoImages.visibility = View.GONE
                android.util.Log.d("SessionDetailsActivity", "Found ${imageFiles.size} local images for PDF generation")
                return
            }
        }
        
        // If no local images, try to download from cloud (session might be from cloud)
        lifecycleScope.launch {
            val session = sessionViewModel.getSessionById(sessionId)
            android.util.Log.d("SessionDetailsActivity", "Local session found: ${session != null}, isUploaded: ${session?.isUploaded}")
            
            // Always try to download from cloud if no local images exist
            // This handles cases where session came from cloud but images aren't downloaded yet
            android.util.Log.d("SessionDetailsActivity", "No local images found, attempting cloud download for: $sessionId")
            
            // Show shimmer while loading cloud images
            showImageShimmer()
            
            // Try to download images from cloud
            val cloudViewModel = ViewModelProvider(this@SessionDetailsActivity)[com.nextserve.oralvishealth.ui.cloud.CloudViewModel::class.java]
            val downloadSuccess = cloudViewModel.downloadSessionImages(sessionId)
            
            android.util.Log.d("SessionDetailsActivity", "Download result: $downloadSuccess")
            
            if (downloadSuccess) {
                // Reload images after download
                loadSessionImagesFromLocal(sessionId)
            } else {
                hideImageShimmer()
                binding.rvImages.visibility = View.GONE
                binding.tvNoImages.visibility = View.VISIBLE
                android.util.Log.e("SessionDetailsActivity", "Failed to download images for session: $sessionId")
            }
        }
    }
    
    private fun loadSessionImagesFromLocal(sessionId: String) {
        val sessionDir = File(getExternalFilesDir(null), "Sessions/$sessionId")
        
        android.util.Log.d("SessionDetailsActivity", "Looking for images in: ${sessionDir.absolutePath}")
        android.util.Log.d("SessionDetailsActivity", "Directory exists: ${sessionDir.exists()}")
        
        if (sessionDir.exists() && sessionDir.isDirectory) {
            val allFiles = sessionDir.listFiles()
            android.util.Log.d("SessionDetailsActivity", "All files in directory: ${allFiles?.map { it.name }}")
            
            val imageFiles = sessionDir.listFiles { file ->
                file.isFile && (file.extension.lowercase() == "jpg" || file.extension.lowercase() == "jpeg" || file.extension.lowercase() == "png")
            }?.toList() ?: emptyList()
            
            android.util.Log.d("SessionDetailsActivity", "Found ${imageFiles.size} image files: ${imageFiles.map { it.name }}")
            
            hideImageShimmer()
            
            if (imageFiles.isNotEmpty()) {
                currentImageFiles = imageFiles
                setupImageRecyclerView(imageFiles)
                binding.rvImages.visibility = View.VISIBLE
                binding.tvNoImages.visibility = View.GONE
            } else {
                currentImageFiles = emptyList()
                binding.rvImages.visibility = View.GONE
                binding.tvNoImages.visibility = View.VISIBLE
            }
        } else {
            android.util.Log.d("SessionDetailsActivity", "Session directory does not exist or is not a directory")
            hideImageShimmer()
            binding.rvImages.visibility = View.GONE
            binding.tvNoImages.visibility = View.VISIBLE
        }
    }
    
    private fun showImageShimmer() {
        // Show shimmer placeholders for images
        binding.rvImages.visibility = View.VISIBLE
        binding.tvNoImages.visibility = View.GONE
        
        // Create shimmer adapter with placeholder items
        val shimmerAdapter = ShimmerImageAdapter(6) // Show 6 shimmer placeholders
        binding.rvImages.adapter = shimmerAdapter
        binding.rvImages.layoutManager = GridLayoutManager(this, 2)
    }
    
    private fun hideImageShimmer() {
        // Will be replaced by actual images or hidden
    }

    private fun setupImageRecyclerView(images: List<File>) {
        imageAdapter = ImageAdapter(images) { imageFile ->
            // Open image preview activity
            val intent = android.content.Intent(this, com.nextserve.oralvishealth.ui.image.ImagePreviewActivity::class.java)
            intent.putExtra("imagePath", imageFile.absolutePath)
            startActivity(intent)
        }
        
        binding.rvImages.apply {
            adapter = imageAdapter
            layoutManager = GridLayoutManager(this@SessionDetailsActivity, 2)
        }
    }
    
    private fun setupReportGeneration() {
        binding.btnGenerateReport.setOnClickListener {
            generatePDFReport()
        }
        
        binding.btnShareWhatsApp.setOnClickListener {
            shareToWhatsApp()
        }
        
        binding.btnShareEmail.setOnClickListener {
            shareToEmail()
        }
    }
    
    private fun generatePDFReport() {
        lifecycleScope.launch {
            try {
                val session = sessionViewModel.getSessionById(currentSessionId ?: return@launch)
                if (session == null) {
                    android.util.Log.e("SessionDetailsActivity", "Session not found for PDF generation")
                    return@launch
                }
                
                if (currentImageFiles.isEmpty()) {
                    android.util.Log.w("SessionDetailsActivity", "No images available for PDF report")
                    // Still generate report with metadata only
                }
                
                binding.btnGenerateReport.isEnabled = false
                binding.btnGenerateReport.text = "Generating PDF..."
                
                val pdfService = PDFReportService(this@SessionDetailsActivity)
                val pdfFile = pdfService.generateSessionReport(
                    sessionId = session.sessionId,
                    patientName = session.name,
                    patientAge = session.age.toString(),
                    sessionTimestamp = session.timestamp,
                    imageFiles = currentImageFiles
                )
                
                if (pdfFile != null && pdfFile.exists()) {
                    generatedPdfFile = pdfFile
                    binding.btnGenerateReport.text = "PDF Generated Successfully!"
                    binding.layoutShareOptions.visibility = View.VISIBLE
                    
                    // Add option to view PDF
                    binding.btnGenerateReport.setOnClickListener {
                        openPDF(pdfFile)
                    }
                    
                    android.util.Log.d("SessionDetailsActivity", "PDF generated: ${pdfFile.absolutePath}, Size: ${pdfFile.length()} bytes")
                } else {
                    binding.btnGenerateReport.text = "Failed to Generate PDF"
                    android.util.Log.e("SessionDetailsActivity", "Failed to generate PDF or file doesn't exist")
                }
                
            } catch (e: Exception) {
                android.util.Log.e("SessionDetailsActivity", "Error generating PDF", e)
                binding.btnGenerateReport.text = "Error Generating PDF"
            } finally {
                binding.btnGenerateReport.isEnabled = true
            }
        }
    }
    
    private fun openPDF(pdfFile: File) {
        try {
            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                pdfFile
            )
            
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                android.util.Log.w("SessionDetailsActivity", "No PDF viewer app found")
                // Fallback to share
                shareGeneral()
            }
        } catch (e: Exception) {
            android.util.Log.e("SessionDetailsActivity", "Failed to open PDF", e)
            shareGeneral()
        }
    }
    
    private fun shareToWhatsApp() {
        val pdfFile = generatedPdfFile ?: return
        
        try {
            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                pdfFile
            )
            
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, "OralVisHealth Session Report - ${currentSessionId}")
                setPackage("com.whatsapp")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                android.util.Log.w("SessionDetailsActivity", "WhatsApp not installed")
                shareGeneral()
            }
        } catch (e: Exception) {
            android.util.Log.e("SessionDetailsActivity", "Failed to share to WhatsApp", e)
            // Fallback to general share
            shareGeneral()
        }
    }
    
    private fun shareToEmail() {
        val pdfFile = generatedPdfFile ?: return
        
        try {
            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                pdfFile
            )
            
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "OralVisHealth Session Report - ${currentSessionId}")
                putExtra(Intent.EXTRA_TEXT, "Please find attached the OralVisHealth session report.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(Intent.createChooser(intent, "Share via Email"))
            } else {
                android.util.Log.w("SessionDetailsActivity", "No email app found")
                shareGeneral()
            }
        } catch (e: Exception) {
            android.util.Log.e("SessionDetailsActivity", "Failed to share via email", e)
        }
    }
    
    private fun shareGeneral() {
        val pdfFile = generatedPdfFile ?: return
        
        try {
            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                pdfFile
            )
            
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, "OralVisHealth Session Report - ${currentSessionId}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            startActivity(Intent.createChooser(intent, "Share PDF Report"))
        } catch (e: Exception) {
            android.util.Log.e("SessionDetailsActivity", "Failed to share PDF", e)
        }
    }
}

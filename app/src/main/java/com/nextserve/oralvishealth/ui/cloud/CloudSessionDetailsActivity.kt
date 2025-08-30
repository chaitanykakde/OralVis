package com.nextserve.oralvishealth.ui.cloud

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.nextserve.oralvishealth.databinding.ActivitySessionDetailsBinding
import com.nextserve.oralvishealth.ui.adapter.ShimmerImageAdapter
import com.nextserve.oralvishealth.ui.adapter.ImageAdapter
import com.nextserve.oralvishealth.service.PDFReportService
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class CloudSessionDetailsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySessionDetailsBinding
    private lateinit var cloudViewModel: CloudViewModel
    private lateinit var imageAdapter: ImageAdapter
    private var currentSessionId: String? = null
    private var currentSessionName: String = ""
    private var currentSessionAge: Int = 0
    private var currentSessionTimestamp: Long = 0
    private var currentImageFiles: List<File> = emptyList()
    private var generatedPdfFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySessionDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cloudViewModel = ViewModelProvider(this)[CloudViewModel::class.java]
        
        setupToolbar()
        setupReportGeneration()
        
        // Initially hide the generate report button until images are loaded
        binding.cardGenerateReport.visibility = View.GONE
        
        val sessionId = intent.getStringExtra("sessionId")
        val sessionName = intent.getStringExtra("sessionName")
        val sessionAge = intent.getIntExtra("sessionAge", 0)
        val sessionTimestamp = intent.getLongExtra("sessionTimestamp", 0)
        
        if (sessionId != null) {
            currentSessionId = sessionId
            currentSessionName = sessionName ?: ""
            currentSessionAge = sessionAge
            currentSessionTimestamp = sessionTimestamp
            
            displaySessionInfo(sessionId, sessionName ?: "", sessionAge, sessionTimestamp)
            loadCloudSessionImages(sessionId)
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

    private fun displaySessionInfo(sessionId: String, name: String, age: Int, timestamp: Long) {
        binding.tvSessionIdLabel.text = sessionId
        binding.tvNameLabel.text = name
        binding.tvAgeLabel.text = "$age years"
        
        // Format timestamp
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val date = Date(timestamp)
        
        binding.tvTimestamp.text = dateFormat.format(date)
        binding.tvTime.text = timeFormat.format(date)
    }

    private fun loadCloudSessionImages(sessionId: String) {
        android.util.Log.d("CloudSessionDetailsActivity", "Loading images for cloud session: $sessionId")
        
        // Show shimmer while loading
        showImageShimmer()
        
        lifecycleScope.launch {
            // First run Firebase Storage test to debug connection issues
            val testService = com.nextserve.oralvishealth.service.FirebaseStorageTestService()
            testService.initialize()
            val testResult = testService.testFirebaseStorageAccess()
            android.util.Log.d("CloudSessionDetailsActivity", "Firebase Storage test result: $testResult")
            
            // Download images directly from Firebase Storage
            val downloadSuccess = cloudViewModel.downloadSessionImages(sessionId)
            
            android.util.Log.d("CloudSessionDetailsActivity", "Download result: $downloadSuccess")
            
            if (downloadSuccess) {
                // Load images from temporary download location
                loadImagesFromDownload(sessionId)
            } else {
                hideImageShimmer()
                binding.rvImages.visibility = View.GONE
                binding.tvNoImages.visibility = View.VISIBLE
                android.util.Log.e("CloudSessionDetailsActivity", "Failed to download images for cloud session: $sessionId")
            }
        }
    }
    
    private fun loadImagesFromDownload(sessionId: String) {
        val sessionDir = File(getExternalFilesDir(null), "Sessions/$sessionId")
        
        android.util.Log.d("CloudSessionDetailsActivity", "Looking for downloaded images in: ${sessionDir.absolutePath}")
        android.util.Log.d("CloudSessionDetailsActivity", "Directory exists: ${sessionDir.exists()}")
        
        if (sessionDir.exists() && sessionDir.isDirectory) {
            val allFiles = sessionDir.listFiles()
            android.util.Log.d("CloudSessionDetailsActivity", "All files in directory: ${allFiles?.map { it.name }}")
            
            val imageFiles = sessionDir.listFiles { file ->
                file.isFile && (file.extension.lowercase() == "jpg" || file.extension.lowercase() == "jpeg" || file.extension.lowercase() == "png") && file.name.contains(sessionId)
            }?.toList() ?: emptyList()
            
            android.util.Log.d("CloudSessionDetailsActivity", "Found ${imageFiles.size} image files: ${imageFiles.map { it.name }}")
            
            hideImageShimmer()
            
            if (imageFiles.isNotEmpty()) {
                currentImageFiles = imageFiles
                setupImageRecyclerView(imageFiles)
                binding.rvImages.visibility = View.VISIBLE
                binding.tvNoImages.visibility = View.GONE
                // Show generate report button only when images are loaded
                binding.cardGenerateReport.visibility = View.VISIBLE
            } else {
                currentImageFiles = emptyList()
                binding.rvImages.visibility = View.GONE
                binding.tvNoImages.visibility = View.VISIBLE
                // Hide generate report button when no images
                binding.cardGenerateReport.visibility = View.GONE
            }
        } else {
            android.util.Log.d("CloudSessionDetailsActivity", "Session directory does not exist")
            hideImageShimmer()
            binding.rvImages.visibility = View.GONE
            binding.tvNoImages.visibility = View.VISIBLE
            // Hide generate report button when no images loaded
            binding.cardGenerateReport.visibility = View.GONE
        }
    }
    
    private fun showImageShimmer() {
        binding.rvImages.visibility = View.VISIBLE
        binding.tvNoImages.visibility = View.GONE
        
        val shimmerAdapter = ShimmerImageAdapter(6)
        binding.rvImages.adapter = shimmerAdapter
        binding.rvImages.layoutManager = GridLayoutManager(this, 2)
    }
    
    private fun hideImageShimmer() {
        // Will be replaced by actual images or hidden
    }

    private fun setupImageRecyclerView(images: List<File>) {
        imageAdapter = ImageAdapter(images) { imageFile ->
            val intent = android.content.Intent(this, com.nextserve.oralvishealth.ui.image.ImagePreviewActivity::class.java)
            intent.putExtra("imagePath", imageFile.absolutePath)
            startActivity(intent)
        }
        
        binding.rvImages.apply {
            adapter = imageAdapter
            layoutManager = GridLayoutManager(this@CloudSessionDetailsActivity, 2)
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
                if (currentSessionId == null) {
                    android.util.Log.e("CloudSessionDetailsActivity", "Session ID not available for PDF generation")
                    return@launch
                }
                
                // Wait for all images to be loaded before generating PDF
                if (currentImageFiles.isEmpty()) {
                    android.util.Log.w("CloudSessionDetailsActivity", "Waiting for images to load before generating PDF")
                    // Give some time for images to load
                    kotlinx.coroutines.delay(2000)
                }
                
                binding.btnGenerateReport.isEnabled = false
                binding.btnGenerateReport.text = "Generating PDF..."
                
                val pdfService = PDFReportService(this@CloudSessionDetailsActivity)
                val pdfFile = pdfService.generateSessionReport(
                    sessionId = currentSessionId!!,
                    patientName = currentSessionName,
                    patientAge = currentSessionAge.toString(),
                    sessionTimestamp = currentSessionTimestamp,
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
                    
                    android.util.Log.d("CloudSessionDetailsActivity", "PDF generated: ${pdfFile.absolutePath}, Size: ${pdfFile.length()} bytes")
                } else {
                    binding.btnGenerateReport.text = "Failed to Generate PDF"
                    android.util.Log.e("CloudSessionDetailsActivity", "Failed to generate PDF or file doesn't exist")
                }
                
            } catch (e: Exception) {
                android.util.Log.e("CloudSessionDetailsActivity", "Error generating PDF", e)
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
                android.util.Log.w("CloudSessionDetailsActivity", "No PDF viewer app found")
                shareGeneral()
            }
        } catch (e: Exception) {
            android.util.Log.e("CloudSessionDetailsActivity", "Failed to open PDF", e)
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
                android.util.Log.w("CloudSessionDetailsActivity", "WhatsApp not installed")
                shareGeneral()
            }
        } catch (e: Exception) {
            android.util.Log.e("CloudSessionDetailsActivity", "Failed to share to WhatsApp", e)
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
                android.util.Log.w("CloudSessionDetailsActivity", "No email app found")
                shareGeneral()
            }
        } catch (e: Exception) {
            android.util.Log.e("CloudSessionDetailsActivity", "Failed to share via email", e)
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
            android.util.Log.e("CloudSessionDetailsActivity", "Failed to share PDF", e)
        }
    }
}

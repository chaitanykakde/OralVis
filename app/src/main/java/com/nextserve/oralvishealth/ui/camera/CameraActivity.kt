package com.nextserve.oralvishealth.ui.camera

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.nextserve.oralvishealth.R
import com.nextserve.oralvishealth.databinding.ActivityCameraBinding
import com.nextserve.oralvishealth.ui.adapter.CapturedImageAdapter
import com.nextserve.oralvishealth.ui.dialog.SaveSessionDialogFragment
import com.nextserve.oralvishealth.ui.viewmodel.CameraViewModel
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

// Camera activity for capturing oral health images
// handles camera preview, image capture, and switching between front/back cameras
class CameraActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCameraBinding
    private lateinit var cameraViewModel: CameraViewModel
    
    // camera related stuff
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var capturedImageAdapter: CapturedImageAdapter
    private val capturedImages = mutableListOf<File>()  // local list for thumbnails
    private var cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA  // start with back camera
    private var cameraProvider: ProcessCameraProvider? = null

    // permission launcher for camera access
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startCamera()
        } else {
            // no camera permission = no point staying here
            Toast.makeText(this, getString(R.string.camera_permission_required), Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // setup viewmodel for managing captured images
        cameraViewModel = ViewModelProvider(this)[CameraViewModel::class.java]
        
        // initialize all the UI components
        setupRecyclerView()
        setupObservers()
        setupClickListeners()
        
        // check camera permission and start camera if we have it
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        // background thread for camera operations
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    // setup the horizontal recyclerview for image thumbnails
    private fun setupRecyclerView() {
        capturedImageAdapter = CapturedImageAdapter(capturedImages) { position ->
            removeImage(position)  // callback for when user taps remove button
        }
        
        binding.rvCapturedImages.apply {
            adapter = capturedImageAdapter
            layoutManager = LinearLayoutManager(this@CameraActivity, LinearLayoutManager.HORIZONTAL, false)
        }
        
        updateImageVisibility()
    }

    // observe changes in image count to update UI
    private fun setupObservers() {
        cameraViewModel.imageCount.observe(this) { count ->
            updateImageVisibility()
        }
    }

    // setup all button click listeners
    private fun setupClickListeners() {
        binding.btnCapture.setOnClickListener {
            takePhoto()
        }
        
        binding.btnFinishSession.setOnClickListener {
            if (cameraViewModel.imageCount.value ?: 0 > 0) {
                showSaveSessionDialog()
            } else {
                Toast.makeText(this, "Please capture at least one image", Toast.LENGTH_SHORT).show()
            }
        }
        
        // camera switch button - toggle between front/back
        binding.fabCameraSwitch.setOnClickListener {
            switchCamera()
        }
    }

    // simple permission check for camera
    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(
        this, Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    // initialize camera provider and bind use cases
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(this))
    }

    // bind camera preview and image capture use cases
    private fun bindCameraUseCases() {
        val preview = Preview.Builder()
            .build()
            .also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }

        imageCapture = ImageCapture.Builder().build()

        try {
            // unbind everything first, then bind new config
            cameraProvider?.unbindAll()
            cameraProvider?.bindToLifecycle(
                this, cameraSelector, preview, imageCapture
            )
        } catch (exc: Exception) {
            Toast.makeText(this, "Failed to start camera", Toast.LENGTH_SHORT).show()
        }
    }

    // toggle between front and back camera
    private fun switchCamera() {
        cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
        
        // rebind with new camera selector
        bindCameraUseCases()
        
        val cameraType = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) "Back" else "Front"
        Toast.makeText(this, "$cameraType camera selected", Toast.LENGTH_SHORT).show()
    }

    // capture photo and save to temp directory
    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        // generate unique filename with timestamp
        val name = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())
        val photoFile = File(
            getExternalFilesDir(null),
            "temp_$name.jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exception: ImageCaptureException) {
                    Toast.makeText(this@CameraActivity, "Photo capture failed: ${exception.message}", Toast.LENGTH_SHORT).show()
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    cameraViewModel.addCapturedImage(savedUri)
                    
                    // add to local list for thumbnail display
                    capturedImages.add(photoFile)
                    capturedImageAdapter.notifyItemInserted(capturedImages.size - 1)
                    updateImageVisibility()
                    
                    Toast.makeText(this@CameraActivity, "Image captured!", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    // remove image when user taps the X button on thumbnail
    private fun removeImage(position: Int) {
        if (position in 0 until capturedImages.size) {
            val removedFile = capturedImages[position]
            
            // remove from viewmodel first
            cameraViewModel.removeCapturedImage(position)
            
            // remove from local list and update adapter
            capturedImages.removeAt(position)
            capturedImageAdapter.notifyItemRemoved(position)
            capturedImageAdapter.notifyItemRangeChanged(position, capturedImages.size)
            
            // delete the actual file too
            removedFile.delete()
            
            updateImageVisibility()
            Toast.makeText(this, "Image removed", Toast.LENGTH_SHORT).show()
        }
    }

    // show/hide the recyclerview based on whether we have images
    private fun updateImageVisibility() {
        val hasImages = capturedImages.isNotEmpty()
        binding.rvCapturedImages.visibility = if (hasImages) View.VISIBLE else View.GONE
        binding.tvNoImages.visibility = if (hasImages) View.GONE else View.VISIBLE
    }

    // open save session dialog when user clicks finish
    private fun showSaveSessionDialog() {
        val dialog = SaveSessionDialogFragment.newInstance(cameraViewModel.getCapturedImagesList())
        dialog.show(supportFragmentManager, "SaveSessionDialog")
    }

    // cleanup when activity is destroyed
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}

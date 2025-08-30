package com.nextserve.oralvishealth.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.*
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

// PDF report generation service for oral health sessions
// creates professional looking reports with patient info and images
class PDFReportService(private val context: Context) {
    
    companion object {
        private const val PDF_FOLDER = "OralVisHealth_Reports"
    }
    
    fun generateSessionReport(
        sessionId: String,
        patientName: String,
        patientAge: String,
        sessionTimestamp: Long,
        imageFiles: List<File>
    ): File? {
        try {
            // Create reports directory in app's external files directory
            val appDir = context.getExternalFilesDir(null)
            val reportsDir = File(appDir, PDF_FOLDER)
            if (!reportsDir.exists()) {
                reportsDir.mkdirs()
            }
            
            // Create PDF file
            val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
            val timestamp = dateFormat.format(Date())
            val pdfFile = File(reportsDir, "OralVisHealth_Report_${sessionId}_$timestamp.pdf")
            
            android.util.Log.d("PDFReportService", "Creating PDF at: ${pdfFile.absolutePath}")
            
            val writer = PdfWriter(FileOutputStream(pdfFile))
            val pdfDocument = PdfDocument(writer)
            val document = Document(pdfDocument)
            
            // Add app logo and header
            addHeader(document)
            
            // Add patient information
            addPatientInfo(document, sessionId, patientName, patientAge, sessionTimestamp)
            
            // Add images
            addSessionImages(document, imageFiles)
            
            // Add footer
            addFooter(document)
            
            document.close()
            
            android.util.Log.d("PDFReportService", "PDF report generated: ${pdfFile.absolutePath}")
            return pdfFile
            
        } catch (e: Exception) {
            android.util.Log.e("PDFReportService", "Failed to generate PDF report", e)
            return null
        }
    }
    
    private fun addHeader(document: Document) {
        try {
            // Add app logo
            try {
                val logoDrawable = androidx.core.content.ContextCompat.getDrawable(context, com.nextserve.oralvishealth.R.drawable.app_logo)
                if (logoDrawable != null) {
                    val bitmap = android.graphics.Bitmap.createBitmap(
                        logoDrawable.intrinsicWidth,
                        logoDrawable.intrinsicHeight,
                        android.graphics.Bitmap.Config.ARGB_8888
                    )
                    val canvas = android.graphics.Canvas(bitmap)
                    logoDrawable.setBounds(0, 0, canvas.width, canvas.height)
                    logoDrawable.draw(canvas)
                    
                    // Convert bitmap to byte array
                    val stream = ByteArrayOutputStream()
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
                    val logoData = ImageDataFactory.create(stream.toByteArray())
                    
                    val logoImage = Image(logoData)
                    logoImage.setWidth(UnitValue.createPointValue(120f))
                    logoImage.setHeight(UnitValue.createPointValue(120f))
                    
                    val logoContainer = Paragraph()
                        .add(logoImage)
                        .setTextAlignment(TextAlignment.CENTER)
                    document.add(logoContainer)
                }
            } catch (e: Exception) {
                android.util.Log.w("PDFReportService", "Failed to add logo to PDF", e)
            }
            
            // Add title
            val title = Paragraph("OralVisHealth - Session Report")
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(20f)
                .setBold()
            document.add(title)
            
            val subtitle = Paragraph("Comprehensive Oral Health Analysis")
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(12f)
                .setItalic()
            document.add(subtitle)
            
            // Add separator line
            document.add(Paragraph("\n"))
            
        } catch (e: Exception) {
            android.util.Log.e("PDFReportService", "Failed to add header", e)
        }
    }
    
    private fun addPatientInfo(
        document: Document,
        sessionId: String,
        patientName: String,
        patientAge: String,
        sessionTimestamp: Long
    ) {
        try {
            val infoTitle = Paragraph("Patient Information")
                .setFontSize(16f)
                .setBold()
            document.add(infoTitle)
            
            // Create table for patient info
            val table = Table(UnitValue.createPercentArray(floatArrayOf(30f, 70f)))
                .setWidth(UnitValue.createPercentValue(100f))
            
            table.addCell("Session ID:")
            table.addCell(sessionId)
            
            table.addCell("Patient Name:")
            table.addCell(patientName)
            
            table.addCell("Age:")
            table.addCell("$patientAge years")
            
            val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault())
            table.addCell("Session Date:")
            table.addCell(dateFormat.format(Date(sessionTimestamp)))
            
            val reportDate = SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault())
            table.addCell("Report Generated:")
            table.addCell(reportDate.format(Date()))
            
            document.add(table)
            document.add(Paragraph("\n"))
            
        } catch (e: Exception) {
            android.util.Log.e("PDFReportService", "Failed to add patient info", e)
        }
    }
    
    private fun addSessionImages(document: Document, imageFiles: List<File>) {
        try {
            val imagesTitle = Paragraph("Session Images")
                .setFontSize(16f)
                .setBold()
            document.add(imagesTitle)
            
            if (imageFiles.isEmpty()) {
                document.add(Paragraph("No images available for this session."))
                return
            }
            
            // Add images in a grid layout (2 per row)
            val imageTable = Table(UnitValue.createPercentArray(floatArrayOf(50f, 50f)))
                .setWidth(UnitValue.createPercentValue(100f))
            
            for (i in imageFiles.indices step 2) {
                val leftImage = imageFiles[i]
                val rightImage = if (i + 1 < imageFiles.size) imageFiles[i + 1] else null
                
                // Add left image
                val leftCell = Cell()
                addImageToCell(leftCell, leftImage, i + 1)
                imageTable.addCell(leftCell)
                
                // Add right image or empty cell
                val rightCell = Cell()
                if (rightImage != null) {
                    addImageToCell(rightCell, rightImage, i + 2)
                }
                imageTable.addCell(rightCell)
            }
            
            document.add(imageTable)
            
        } catch (e: Exception) {
            android.util.Log.e("PDFReportService", "Failed to add session images", e)
        }
    }
    
    private fun addImageToCell(cell: Cell, imageFile: File, imageNumber: Int) {
        try {
            if (imageFile.exists()) {
                val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
                if (bitmap != null) {
                    // Resize bitmap to fit in PDF
                    val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 200, 200, true)
                    
                    // Convert bitmap to byte array
                    val stream = ByteArrayOutputStream()
                    resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
                    val imageData = ImageDataFactory.create(stream.toByteArray())
                    
                    val image = Image(imageData)
                    image.setWidth(UnitValue.createPointValue(150f))
                    image.setHeight(UnitValue.createPointValue(150f))
                    
                    cell.add(image)
                    cell.add(Paragraph("Image $imageNumber").setTextAlignment(TextAlignment.CENTER).setFontSize(10f))
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("PDFReportService", "Failed to add image to cell", e)
            cell.add(Paragraph("Image $imageNumber\n(Failed to load)").setTextAlignment(TextAlignment.CENTER))
        }
    }
    
    private fun addFooter(document: Document) {
        try {
            document.add(Paragraph("\n"))
            
            val footer = Paragraph("Generated by OralVisHealth App")
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(10f)
                .setItalic()
            document.add(footer)
            
            val disclaimer = Paragraph("This report is for informational purposes only. Please consult with a qualified healthcare professional for medical advice.")
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(8f)
                .setItalic()
            document.add(disclaimer)
            
        } catch (e: Exception) {
            android.util.Log.e("PDFReportService", "Failed to add footer", e)
        }
    }
}

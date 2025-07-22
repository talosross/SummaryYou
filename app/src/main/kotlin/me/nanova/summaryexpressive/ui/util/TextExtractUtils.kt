package me.nanova.summaryexpressive.ui.util

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.graphics.createBitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.FileNotFoundException

suspend fun extractTextFromPdf(context: Context, selectedPdfUri: Uri): String {
    // Open the PDF file
    val pdfRenderer = PdfRenderer(context.contentResolver.openFileDescriptor(selectedPdfUri, "r")!!)

    // Initialize the text recognizer
    val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    // Initialize the StringBuilder
    val extractedText = StringBuilder()

    // Iterate through the pages of the PDF
    for (pageNumber in 0 until pdfRenderer.pageCount) {
        // Get the page as an image
        val page = pdfRenderer.openPage(pageNumber)
        val pageImage = createBitmap(page.width, page.height)
        page.render(pageImage, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

        // Create an input image from the page image
        val inputImage = InputImage.fromBitmap(pageImage, 0)

        // Recognize text from the page image
        val result = textRecognizer.process(inputImage).await()
        extractedText.append(result.text)

        // Close the page
        page.close()
    }

    // Close the PDF file
    pdfRenderer.close()

    // Return the extracted text
    return extractedText.toString()
}

suspend fun extractTextFromDocx(context: Context, selectedDocxUri: Uri): String =
    withContext(Dispatchers.IO) {
        context.contentResolver.openInputStream(selectedDocxUri)?.use { inputStream ->
            XWPFDocument(inputStream).use { doc ->
                val extractedText = StringBuilder()
                doc.paragraphs.forEach { paragraph ->
                    extractedText.append(paragraph.text).append("\n")
                }
                extractedText.toString()
            }
        }
            ?: throw FileNotFoundException("Can't open InputStream for the URI: $selectedDocxUri")
    }

suspend fun extractTextFromImage(context: Context, selectedImageUri: Uri): String =
    withContext(Dispatchers.IO) {
        // Initialize the text recognizer
        val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        // Load the image from the URI
        val inputImage = InputImage.fromFilePath(context, selectedImageUri)

        // Recognize text from the image
        val result = textRecognizer.process(inputImage).await()

        // Return the extracted text
        result.text
    }

fun getFileName(context: Context, uri: Uri): String {
    var name = ""
    val cursor = context.contentResolver.query(uri, null, null, null, null)
    cursor?.let {
        it.moveToFirst()
        val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (index != -1) {
            name = cursor.getString(index)
        }
        it.close()
    }
    return name
}
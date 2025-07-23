package me.nanova.summaryexpressive.ui.util

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Xml
import androidx.core.graphics.createBitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.FileNotFoundException
import java.io.InputStream
import java.util.zip.ZipInputStream

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

private fun parseDocxXml(xmlInputStream: InputStream): String {
    val parser = Xml.newPullParser().apply {
        setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        setInput(xmlInputStream, null)
    }

    val extractedText = StringBuilder()
    var eventType = parser.eventType
    while (eventType != XmlPullParser.END_DOCUMENT) {
        when (eventType) {
            XmlPullParser.START_TAG -> {
                if (parser.name == "w:t" && parser.next() == XmlPullParser.TEXT) {
                    extractedText.append(parser.text)
                }
            }

            XmlPullParser.END_TAG -> {
                if (parser.name == "w:p") {
                    extractedText.append("\n")
                }
            }
        }
        eventType = parser.next()
    }
    return extractedText.toString().trim()
}

suspend fun extractTextFromDocx(context: Context, selectedDocxUri: Uri): String =
    withContext(Dispatchers.IO) {
        val inputStream = context.contentResolver.openInputStream(selectedDocxUri)
            ?: throw FileNotFoundException("Can't open InputStream for the URI: $selectedDocxUri")

        inputStream.use { stream ->
            ZipInputStream(stream).use { zis ->
                generateSequence { zis.nextEntry }
                    .find { it.name == "word/document.xml" }
                    ?.let {
                        return@withContext parseDocxXml(zis)
                    }
            }
        }

        throw FileNotFoundException("word/document.xml not found in the DOCX file.")
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
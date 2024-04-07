package com.talosross.summaryyou

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Resources
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imeNestedScroll
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.AccessibilityAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import coil.size.Size
import com.chaquo.python.PyException
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.talosross.summaryyou.ui.theme.SummaryYouTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.UUID

class MainActivity : ComponentActivity() {
    private var sharedUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!Python.isStarted()) { //If not started, python will start here
            Python.start(AndroidPlatform(this))
        }
        // Access the applicationContext and pass it to ViewModel class
        val viewModel = TextSummaryViewModel(applicationContext)

        // Lay app behind the system bars
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Check if a link has been shared
        val intent: Intent? = intent
        if (Intent.ACTION_SEND == intent?.action && intent.type == "text/plain") {
            sharedUrl = intent.getStringExtra(Intent.EXTRA_TEXT)
        }

        setContent {
            SummaryYouTheme(design = viewModel.getDesignNumber(), OledModeEnabled = viewModel.getUltraDarkValue()) {
                val navController = rememberNavController()
                var shouldShowOnboarding by rememberSaveable { mutableStateOf(viewModel.getShowOnboardingScreenValue()) }
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (shouldShowOnboarding) {
                        OnboardingScreen(onContinueClicked = {
                            shouldShowOnboarding = false
                            viewModel.setShowOnboardingScreenValue(false)
                        })
                    } else {
                        AppNavigation(navController, applicationContext, sharedUrl)
                    }
                }
            }
        }
    }
}

class TextSummaryViewModel(private val context: Context) : ViewModel() {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("TextSummaries", Context.MODE_PRIVATE)

    val textSummaries = mutableStateListOf<TextSummary>()

    init {
        // When creating the ViewModel, retrieve the saved data (if any)
        val savedTextSummaries = sharedPreferences.getString("textSummaries", null)
        savedTextSummaries?.let {
            val type = object : TypeToken<List<TextSummary>>() {}.type
            val textSummariesFromJson = Gson().fromJson<List<TextSummary>>(it, type)
            textSummaries.addAll(textSummariesFromJson)
        }
    }

    fun addTextSummary(title: String?, author: String?, text: String?, youtubeLink: Boolean) {
        val nonNullTitle = title ?: ""
        val nonNullAuthor = author ?: ""

        if (text != null && text.isNotBlank() && text != "invalid link") {
            val uniqueId = UUID.randomUUID().toString()
            val newTextSummary = TextSummary(uniqueId, nonNullTitle, nonNullAuthor, text, youtubeLink)
            textSummaries.add(newTextSummary)
            // Save text data in SharedPreferences
            saveTextSummaries()
        }
    }

    private fun saveTextSummaries() {
        val textSummariesJson = Gson().toJson(textSummaries)
        sharedPreferences.edit().putString("textSummaries", textSummariesJson).apply()
    }

    fun removeTextSummary(id: String) {
        // Find the TextSummary object to remove based on id
        val textSummaryToRemove = textSummaries.firstOrNull { it.id == id }

        // Check whether a matching TextSummary object was found and remove it
        textSummaryToRemove?.let {
            textSummaries.remove(it)

            // After removing, save the updated ViewModel (if necessary)
            saveTextSummaries()
        }
    }

    fun searchTextSummary(searchText: String): List<String> {
        return textSummaries
            .filter { it.title.contains(searchText, ignoreCase = true) or it.author.contains(searchText, ignoreCase = true) or it.text.contains(searchText, ignoreCase = true)  }
            .map { it.id }
            .takeIf { it.isNotEmpty() }
            ?.toList()
            ?: emptyList()
    }

    fun getAllTextSummaries(): List<String> {
        return textSummaries
            .map { it.id }
            .takeIf { it.isNotEmpty() }
            ?.toList()
            ?: emptyList()
    }


    // Original Language in summary
    var useOriginalLanguage by mutableStateOf(sharedPreferences.getBoolean("useOriginalLanguage", false))

    fun setUseOriginalLanguageValue(newValue: Boolean) {
        useOriginalLanguage = newValue
        sharedPreferences.edit().putBoolean("useOriginalLanguage", newValue).apply()
    }

    fun getUseOriginalLanguageValue(): Boolean {
        return useOriginalLanguage
    }

    // Multiline URL-Field
    var multiLine by mutableStateOf(sharedPreferences.getBoolean("multiLine", true))

    fun setMultiLineValue(newValue: Boolean) {
        multiLine = newValue
        sharedPreferences.edit().putBoolean("multiLine", newValue).apply()
    }

    fun getMultiLineValue(): Boolean {
        return multiLine
    }

    // UltraDark - Mode
    var ultraDark by mutableStateOf(sharedPreferences.getBoolean("ultraDark", false))

    fun setUltraDarkValue(newValue: Boolean) {
        ultraDark = newValue
        sharedPreferences.edit().putBoolean("ultraDark", newValue).apply()
    }

    fun getUltraDarkValue(): Boolean {
        return ultraDark
    }

    // DesignNumber for Dark, Light or System
    private val _designNumberLiveData = MutableLiveData<Int>()

    val designNumber: LiveData<Int> = _designNumberLiveData

    init {
        // Beim Erstellen des ViewModels, setze den aktuellen Wert aus den SharedPreferences
        _designNumberLiveData.value = sharedPreferences.getInt("designNumber", 0)
    }

    fun setDesignNumber(newValue: Int) {
        _designNumberLiveData.value = newValue
        sharedPreferences.edit().putInt("designNumber", newValue).apply()
    }

    fun getDesignNumber(): Int {
        return _designNumberLiveData.value ?: 1 // Standardwert 1, wenn der Wert null ist
    }

    // API Key
    var apiKey by mutableStateOf(sharedPreferences.getString("apiKey", null))

    fun setApiKeyValue(newValue: String?) {
        apiKey = newValue
        sharedPreferences.edit().putString("apiKey", newValue).apply()
    }

    fun getApiKeyValue(): String? {
        return apiKey
    }

    // AI-Model
    var model by mutableStateOf(sharedPreferences.getString("model", "Groq"))

    fun setModelValue(newValue: String?) {
        model = newValue
        sharedPreferences.edit().putString("model", newValue).apply()
    }

    fun getModelValue(): String? {
        return model
    }

    // OnboardingScreen
    var showOnboardingScreen by mutableStateOf(sharedPreferences.getBoolean("showOnboardingScreen", true))

    fun setShowOnboardingScreenValue(newValue: Boolean) {
        showOnboardingScreen = newValue
        sharedPreferences.edit().putBoolean("showOnboardingScreen", newValue).apply()
    }

    fun getShowOnboardingScreenValue(): Boolean {
        return showOnboardingScreen
    }
}


data class TextSummary(val id: String, val title: String, val author: String, val text: String, val youtubeLink: Boolean)

@Composable
fun AppNavigation(navController: NavHostController, applicationContext: Context, initialUrl: String? = null) {
    val viewModel = TextSummaryViewModel(applicationContext) //For History
    NavHost(navController, startDestination = "home") {
        composable("home") {
            homeScreen(
                modifier = Modifier,
                navController = navController,
                viewModel = viewModel,
                initialUrl
            )
        }
        composable("settings") {
            settingsScreen(
                modifier = Modifier,
                navController = navController,
                viewModel = viewModel
            )
        }
        composable("history") {
            historyScreen(
                modifier = Modifier,
                navController = navController,
                viewModel = viewModel
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class,
    ExperimentalLayoutApi::class
)
@Composable
fun homeScreen(modifier: Modifier = Modifier, navController: NavHostController, viewModel: TextSummaryViewModel, initialUrl: String? = null) {
    var transcriptResult by remember { mutableStateOf<String?>(null) } // State for the transcript retrieval result
    var title by remember { mutableStateOf<String?>(null) }
    var author by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) } // For Loading-Animation
    var url by remember { mutableStateOf(initialUrl ?: "") }
    val scope = rememberCoroutineScope() // Python needs asynchronous call
    val context = LocalContext.current // Clipboard
    val haptics = LocalHapticFeedback.current // Vibrations
    val focusManager = LocalFocusManager.current // Hide cursor
    val focusRequester = remember { FocusRequester() } // Show cursor after removing
    var selectedIndex by remember { mutableStateOf(0) } // Summary length index
    val options = listOf(stringResource(id = R.string.short_length), stringResource(id = R.string.middle_length), stringResource(id = R.string.long_length)) // Lengths
    val showCancelIcon by remember { derivedStateOf { url.isNotBlank() } }
    var isError by remember { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
    val key: String = APIKeyLibrary.getAPIKey()
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }


    val clipboardManager = ContextCompat.getSystemService(
        context,
        ClipboardManager::class.java
    ) as ClipboardManager

    /*
    fun extractTextFromDocument(uri: Uri) {
        val inputStream = context.contentResolver.openInputStream(uri)
        val fileData = inputStream?.readBytes()

        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val image = InputImage.fromByteArray(fileData, 0, fileData.size, 0, InputImage.IMAGE_FORMAT_JPEG)
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val extractedText = visionText.text
                Log.d("TextExtraction", "Extracted Text: $extractedText")
                // Verarbeite den extrahierten Text weiter
            }
            .addOnFailureListener { e ->
                Log.e("TextExtraction", "Text extraction failed", e)
                // Behandle den Fehler
                // ...
            }
    }
    */
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        selectedFileUri = uri
        //selectedFileUri?.let { fileUri ->
        //    extractTextFromDocument(fileUri)
        //}
    }

    fun openFilePicker() {
       launcher.launch(arrayOf("*/*"))
    }

    Box {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                LargeTopAppBar(
                    modifier = Modifier
                        .height(110.dp),
                    colors = TopAppBarDefaults.topAppBarColors(
                    ),
                    title = {

                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { navController.navigate("settings") }
                        ) {
                            Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { navController.navigate("history") }
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.outline_library_books_24),
                                contentDescription = "History"
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior
                )
            },
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize(),
                contentPadding = innerPadding,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Column(
                        modifier = modifier
                            .fillMaxSize()
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {

                        Column(
                            modifier = modifier
                                .fillMaxSize()
                                .padding(top = 50.dp, start = 20.dp, end = 20.dp)
                        ) {
                            Text(
                                text = "Summary You",
                                style = MaterialTheme.typography.headlineLarge
                            )
                            // Loading-Animation
                            if (isLoading) {
                                LinearProgressIndicator(
                                    modifier = modifier
                                        .fillMaxWidth()
                                        .padding(top = 5.dp)
                                )
                            } else {
                                Spacer(modifier = modifier.height(height = 9.dp))
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = url,
                                    onValueChange = { url = it },
                                    label = { Text("URL/Text") },
                                    isError = isError,
                                    supportingText = {
                                        if (isError) {
                                            Text(
                                                modifier = Modifier.fillMaxWidth(),
                                                text = when (transcriptResult) {
                                                    "Exception: no internet" -> stringResource(id = R.string.noInternet)
                                                    "Exception: invalid link" -> stringResource(id = R.string.invalidURL)
                                                    "Exception: no transcript" -> stringResource(id = R.string.noTranscript)
                                                    "Exception: no content" -> stringResource(id = R.string.noContent)
                                                    "Exception: invalid input" -> stringResource(id = R.string.invalidInput)
                                                    "Exception: paywall detected" -> stringResource(id = R.string.paywallDetected)
                                                    "Exception: too long" -> stringResource(id = R.string.tooLong)
                                                    "Exception: incorrect key" -> {
                                                        if (key.isEmpty()) {
                                                            stringResource(id = R.string.incorrectKeyOpenSource)
                                                        } else {
                                                            stringResource(id = R.string.incorrectKey)
                                                        }
                                                    }
                                                    "Exception: rate limit" -> stringResource(id = R.string.rateLimit)
                                                    "Exception: no key" -> stringResource(id = R.string.noKey)
                                                    else -> transcriptResult ?: "unknown error 3"
                                                },
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    },
                                    trailingIcon = {
                                        if (showCancelIcon) {
                                            IconButton(
                                                onClick = {
                                                    url = ""
                                                    transcriptResult = null
                                                    isError = false // No error
                                                    focusRequester.requestFocus()
                                                }
                                            ) {
                                                Icon(
                                                    painter = painterResource(id = R.drawable.outline_cancel_24),
                                                    contentDescription = "Cancel"
                                                )
                                            }
                                        }
                                    },
                                    singleLine = !viewModel.getMultiLineValue(),
                                    modifier = modifier
                                        .weight(1f)
                                        .padding(top = 20.dp)
                                        .focusRequester(focusRequester)
                                )
                                /*
                                Spacer(modifier = Modifier.width(16.dp))
                                OutlinedButton(
                                    onClick = { openFilePicker() },
                                    modifier = modifier
                                        .height(72.dp)
                                        .padding(top = 15.dp)
                                ) {
                                    Icon(Icons.Filled.AddCircle, "Floating action button.")
                                }
                                */
                            }
                            Box(
                                modifier = if (isError) {
                                    Modifier.padding(top = 11.dp)
                                } else {
                                    Modifier.padding(top = 15.dp)
                                }
                            ) {
                                SingleChoiceSegmentedButtonRow(modifier.fillMaxWidth()) {
                                    options.forEachIndexed { index, label ->
                                        SegmentedButton(
                                            shape = SegmentedButtonDefaults.itemShape(
                                                index = index,
                                                count = options.size
                                            ),
                                            onClick = { selectedIndex = index },
                                            selected = index == selectedIndex
                                        ) {
                                            Text(label)
                                        }
                                    }
                                }
                            }
                            if (!transcriptResult.isNullOrEmpty() && isError == false) {
                                Card(
                                    modifier = modifier
                                        .padding(top = 15.dp, bottom = 15.dp)
                                        .fillMaxWidth()
                                        .combinedClickable(
                                            onClick = {},
                                            onLongClick = {
                                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                // Copy the contents of the box to the clipboard
                                                clipboardManager.setPrimaryClip(
                                                    ClipData.newPlainText(null, transcriptResult)
                                                )
                                            }
                                        )
                                ) {
                                    if (!title.isNullOrEmpty()) {
                                        Text(
                                            text = title ?: "",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            modifier = modifier
                                                .padding(top = 12.dp, start = 12.dp, end = 12.dp)
                                        )
                                        if (!author.isNullOrEmpty()) {
                                            Row {
                                                Text(
                                                    text = author ?: "",
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    modifier = modifier
                                                        .padding(
                                                            top = 4.dp,
                                                            start = 12.dp,
                                                            end = 12.dp
                                                        )
                                                )
                                                if (isYouTubeLink(url)) {
                                                    Icon(
                                                        painter = painterResource(id = R.drawable.youtube),
                                                        contentDescription = null,
                                                        modifier = Modifier.padding(top = 1.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    Text(
                                        text = transcriptResult
                                            ?: stringResource(id = R.string.noTranscript),
                                        style = MaterialTheme.typography.labelLarge,
                                        modifier = modifier
                                            .padding(
                                                start = 12.dp,
                                                end = 12.dp,
                                                top = 10.dp,
                                                bottom = 12.dp
                                            )
                                    )
                                    var tts: TextToSpeech? by remember { mutableStateOf(null) }
                                    var isSpeaking by remember { mutableStateOf(false) }
                                    var isPaused by remember { mutableStateOf(false) }
                                    var currentPosition by remember { mutableStateOf(0) }
                                    var utteranceId by remember { mutableStateOf("") }
                                    val copied = stringResource(id = R.string.copied)

                                    val utteranceProgressListener = object : UtteranceProgressListener() {
                                        override fun onStart(utteranceId: String) {
                                            // Is called when an utterance starts
                                        }

                                        override fun onDone(utteranceId: String) {
                                            // Is called when an utterance is done
                                            currentPosition = 0
                                            isSpeaking = false
                                            isPaused = false
                                        }

                                        override fun onError(utteranceId: String) {
                                            // Is called when an error occurs
                                        }

                                        override fun onRangeStart(utteranceId: String, start: Int, end: Int, frame: Int) {
                                            // Is called when a new range of text is being spoken
                                            currentPosition = end
                                        }
                                    }
                                    tts?.setOnUtteranceProgressListener(utteranceProgressListener)
                                    DisposableEffect(Unit) {
                                        tts = TextToSpeech(context) { status ->
                                            if (status == TextToSpeech.SUCCESS) {
                                                // TTS-Engine successfully initialized
                                                Log.d("TTS", "Text-to-Speech engine was successfully initialized.")
                                            } else {
                                                // Error initializing the TTS-Engine
                                                Log.d("TTS", "Error initializing the Text-to-Speech engine.")
                                            }
                                        }
                                        onDispose {
                                            tts?.stop()
                                            tts?.shutdown()
                                        }
                                    }

                                    Row {
                                        IconButton(
                                            onClick = {
                                                if (isSpeaking) {
                                                    tts?.stop()
                                                    isSpeaking = false
                                                    isPaused = false
                                                    currentPosition = 0
                                                } else {
                                                    val transcript = transcriptResult
                                                    if (transcript != null) {
                                                        utteranceId = UUID.randomUUID().toString()
                                                        tts?.speak(transcript, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
                                                        isSpeaking = true
                                                    }
                                                }
                                            }
                                        ) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.outline_volume_up_24),
                                                contentDescription = if (isSpeaking) "Beenden" else "Vorlesen"
                                            )
                                        }

                                        AnimatedVisibility(visible = isSpeaking) {
                                            IconButton(
                                                onClick = {
                                                    if (isPaused) {
                                                        val transcript = transcriptResult
                                                        if (transcript != null) {
                                                            val remainingText = transcript.substring(currentPosition)
                                                            utteranceId = UUID.randomUUID().toString()
                                                            tts?.speak(remainingText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
                                                            isPaused = false
                                                        }
                                                    } else {
                                                        tts?.stop()
                                                        isPaused = true
                                                    }
                                                }
                                            ) {
                                                Icon(
                                                    painter = if (isPaused) {
                                                        painterResource(id = R.drawable.outline_play_circle_filled_24)
                                                    } else {
                                                        painterResource(id = R.drawable.outline_pause_circle_filled_24)
                                                    },
                                                    contentDescription = if (isPaused) "Fortsetzen" else "Pausieren"
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.weight(1f))

                                        IconButton(
                                            onClick = {
                                                clipboardManager.setPrimaryClip(
                                                    ClipData.newPlainText(null, transcriptResult)
                                                )
                                                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                                                    Toast.makeText(context, copied, Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        ) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.outline_content_copy_24),
                                                contentDescription = "Kopieren"
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                    type = "text/plain"
                                                    putExtra(Intent.EXTRA_TEXT, transcriptResult)
                                                }
                                                val chooserIntent = Intent.createChooser(shareIntent, null)
                                                context.startActivity(chooserIntent)
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Share,
                                                contentDescription = "Teilen"
                                            )
                                        }
                                    }
                                }
                                Column(
                                    modifier = Modifier
                                        .padding(top = 15.dp, bottom = 90.dp)
                                        .fillMaxSize(),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Button(
                                        onClick = {
                                            focusManager.clearFocus()
                                            isLoading = true // Start Loading-Animation
                                            isError = false // No error
                                            scope.launch {
                                                title = getTitel(url)
                                                author = getAuthor(url)
                                                val (result, error) = summarize(
                                                    url,
                                                    selectedIndex,
                                                    viewModel
                                                )
                                                transcriptResult = result
                                                isError = error
                                                isLoading = false // Stop Loading-Animation
                                                if(!isError){
                                                    if (isYouTubeLink(url)) {
                                                        viewModel.addTextSummary(title, author, transcriptResult, true) // Add to history
                                                    }else{
                                                        viewModel.addTextSummary(title, author, transcriptResult, false) // Add to history
                                                    }
                                                }
                                            }
                                        },
                                        contentPadding = ButtonDefaults.ButtonWithIconContentPadding
                                    ) {
                                        Icon(
                                            Icons.Filled.Refresh,
                                            contentDescription = "Refresh",
                                            modifier = Modifier.size(ButtonDefaults.IconSize)
                                        )
                                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                        Text(stringResource(id = R.string.regenerate))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    Box(
        modifier = Modifier
            .imePadding()
            .imeNestedScroll()
            .fillMaxSize(),
        contentAlignment = Alignment.BottomEnd
    ) {
        Column {
            FloatingActionButton(
                onClick = {
                    scope.launch {
                        // Check if there is anything on the clipboard
                        val clipData = clipboardManager.primaryClip
                        if (clipData != null && clipData.itemCount > 0) {
                            val clipItem = clipData.getItemAt(0)
                            url = clipItem.text.toString()
                        }
                    }
                },
                modifier = modifier.padding(bottom = 20.dp, end = 15.dp)
            ) {
                Icon(painter = painterResource(id = R.drawable.outline_content_paste_24), "Paste")
            }
            FloatingActionButton(
                onClick = {
                    focusManager.clearFocus()
                    isLoading = true // Start Loading-Animation
                    if(isError){transcriptResult = ""}
                    isError = false // No error
                    scope.launch {
                        title = getTitel(url)
                        author = getAuthor(url)
                        val (result, error) = summarize(url, selectedIndex, viewModel)
                        transcriptResult = result
                        isError = error
                        isLoading = false // Stop Loading-Animation
                        if(!isError){
                            if (isYouTubeLink(url)) {
                                viewModel.addTextSummary(title, author, transcriptResult, true) // Add to history
                            }else{
                                viewModel.addTextSummary(title, author, transcriptResult, false) // Add to history
                            }
                        }
                    }
                },
                modifier = modifier.padding(bottom = 60.dp, end = 15.dp)
            ) {
                Icon(Icons.Filled.Check, "Check")
            }
        }
    }
}

suspend fun summarize(text: String, length: Int, viewModel: TextSummaryViewModel): Pair<String, Boolean> {
    val py = Python.getInstance()
    val module = py.getModule("youtube")
    var key: String = APIKeyLibrary.getAPIKey()
    var model: String = viewModel.getModelValue().toString()

    if (key.isEmpty()) {
        key = viewModel.getApiKeyValue().toString()
    }

    // Get the currently set language
    val currentLocale: Locale = Resources.getSystem().configuration.locale

    val language: String = if (viewModel.getUseOriginalLanguageValue()) {
        "the same language as the "
    } else {
        currentLocale.getDisplayLanguage(Locale.ENGLISH)
    }

    try {
        val result = withContext(Dispatchers.IO) {
            module.callAttr("summarize", text, length, language, key, model).toString()
        }
        return Pair(result, false)
    } catch (e: PyException) {
        return Pair(e.message ?: "unknown error 2", true)
    } catch (e: Exception) {
        return Pair(e.message ?: "unknown error 3", true)
    }
}



suspend fun getAuthor(url: String): String? {
    val py = Python.getInstance()
    val module = py.getModule("youtube")

    try {
        val result = withContext(Dispatchers.IO) {
            module.callAttr("get_author", url).toString()
        }
        return result
    } catch (e: Exception) {
        //return "Error getting author"
        return null
    }
}

suspend fun getTitel(url: String): String? {
    val py = Python.getInstance()
    val module = py.getModule("youtube")

    try {
        val result = withContext(Dispatchers.IO) {
            module.callAttr("get_title", url).toString()
        }
        return result
    } catch (e: Exception) {
        //return "Error getting title"
        return null
    }
}

fun isYouTubeLink(input: String): Boolean {
    val youtubePattern = Regex("""^(https?://)?(www\.)?(youtube\.com|youtu\.be)/.*$""")
    return youtubePattern.matches(input)
}




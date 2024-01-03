package com.talosross.summaryyou

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Resources
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.chaquo.python.PyException
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(navController, applicationContext, sharedUrl)
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
    var multiLine by mutableStateOf(sharedPreferences.getBoolean("multiLine", false))

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

    val clipboardManager = ContextCompat.getSystemService(
        context,
        ClipboardManager::class.java
    ) as ClipboardManager

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
                            OutlinedTextField(
                                value = url,
                                onValueChange = { url = it },
                                label = { Text("URL") },
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
                                                "Exception: paywall detected" -> stringResource(id = R.string.paywallDetected)
                                                "Exception: incorrect api" -> {
                                                    if (key.isEmpty()) {
                                                        stringResource(id = R.string.incorrectApiOpenSource)
                                                    } else {
                                                        stringResource(id = R.string.incorrectApi)
                                                    }
                                                }
                                                "Exception: no api" -> stringResource(id = R.string.noApi)
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
                                    .fillMaxWidth()
                                    .padding(top = 20.dp)
                                    .focusRequester(focusRequester)
                            )
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

suspend fun summarize(url: String, length: Int, viewModel: TextSummaryViewModel): Pair<String, Boolean> {
    val py = Python.getInstance()
    val module = py.getModule("youtube")
    var key: String = APIKeyLibrary.getAPIKey()

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
            module.callAttr("summarize", url, key, length, language).toString()
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



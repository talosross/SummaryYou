package com.example.summaryyoupython

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.chaquo.python.*
import com.chaquo.python.android.AndroidPlatform
import com.example.summaryyoupython.ui.theme.SummaryYouPythonTheme
import io.github.cdimascio.dotenv.dotenv
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.navigation.NavHostController
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class MainActivity : ComponentActivity() {
    private var sharedUrl: String? = null // Deklariere sharedUrl hier

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }
        // Hier kannst du auf den applicationContext zugreifen und ihn an deine ViewModel-Klasse übergeben
        val viewModel = TextSummaryViewModel(applicationContext)
        // This will lay out our app behind the system bars
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Überprüfe, ob ein Link geteilt wurde
        val intent: Intent? = intent
        if (Intent.ACTION_SEND == intent?.action && intent.type == "text/plain") {
            sharedUrl = intent.getStringExtra(Intent.EXTRA_TEXT)
        }
        setContent {
            SummaryYouPythonTheme {
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
        // Beim Erstellen des ViewModels die gespeicherten Daten abrufen (wenn vorhanden)
        val savedTextSummaries = sharedPreferences.getString("textSummaries", null)
        savedTextSummaries?.let {
            val type = object : TypeToken<List<TextSummary>>() {}.type
            val textSummariesFromJson = Gson().fromJson<List<TextSummary>>(it, type)
            textSummaries.addAll(textSummariesFromJson)
        }
    }

    fun addTextSummary(title: String?, author: String?, text: String?) {
        val nonNullTitle = title ?: ""
        val nonNullAuthor = author ?: ""

        if (text != null && text.isNotBlank() && text != "ungültiger Link") {
            val newTextSummary = TextSummary(nonNullTitle, nonNullAuthor, text)
            textSummaries.add(newTextSummary)
            // Textdaten in den SharedPreferences speichern
            saveTextSummaries()
        }
    }

    private fun saveTextSummaries() {
        val textSummariesJson = Gson().toJson(textSummaries)
        sharedPreferences.edit().putString("textSummaries", textSummariesJson).apply()
    }

    fun removeTextSummary(title: String?, author: String?, text: String?) {
        // Finde das TextSummary-Objekt, das entfernt werden soll, basierend auf title, author und text
        val textSummaryToRemove = textSummaries.firstOrNull { it.title == title && it.author == author && it.text == text }

        // Überprüfe, ob ein passendes TextSummary-Objekt gefunden wurde, und entferne es
        textSummaryToRemove?.let {
            textSummaries.remove(it)

            // Nach dem Entfernen das aktualisierte ViewModel speichern (falls erforderlich)
            saveTextSummaries()
        }
    }

}


data class TextSummary(val title: String, val author: String, val text: String)


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
                navController = navController
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

@Composable
fun historyScreen(modifier: Modifier = Modifier, navController: NavHostController, viewModel: TextSummaryViewModel) {

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        IconButton(onClick = {navController.navigate("home")}, modifier = modifier.padding(start = 8.dp, top=55.dp)) {
            Icon(Icons.Outlined.ArrowBack, contentDescription = "Localized description")
        }
        Column(
            modifier = modifier
                .padding(start=20.dp, end=20.dp, top=17.dp)
        ) {
            Text(
                text = "Bibliothek",
                style = MaterialTheme.typography.headlineLarge
            )
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (textSummary in viewModel.textSummaries.reversed()) {
                    Textbox(
                        modifier = Modifier,
                        title = textSummary.title,
                        author = textSummary.author,
                        text = textSummary.text,
                        viewModel = viewModel
                    )
                }
            }

        }
    }
}


@Composable
fun settingsScreen(modifier: Modifier = Modifier, navController: NavHostController) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        IconButton(onClick = {navController.navigate("home")}, modifier = modifier.padding(start = 8.dp, top=55.dp)) {
            Icon(Icons.Outlined.ArrowBack, contentDescription = "Localized description")
        }
        Column(
            modifier = modifier
                .padding(start=20.dp, end=20.dp, top=17.dp)
        ) {
            Text(
                text = "Einstellungen",
                style = MaterialTheme.typography.headlineLarge
            )

        }
    }
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class,
    ExperimentalLayoutApi::class
)
@Composable
fun homeScreen(modifier: Modifier = Modifier, navController: NavHostController, viewModel: TextSummaryViewModel, initialUrl: String? = null) {
    // Zustand für das Ergebnis des Transkript-Abrufs
    var transcriptResult by remember { mutableStateOf<String?>(null) }
    var title by remember { mutableStateOf<String?>(null) }
    var author by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var url by remember { mutableStateOf(initialUrl ?: "") }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current // Zugriff auf den Context
    val haptics = LocalHapticFeedback.current //Vibration bei kopieren von Zusammenfassung
    val focusManager = LocalFocusManager.current //Cursor ausblenden
    val focusRequester = remember { FocusRequester() } //Cursor einblenden nach entfernen
    var selectedIndex by remember { mutableStateOf(0) } //Index für Zusammenfassungslänge
    val options = listOf("Kurz", "Mittel", "Lang") //Längen
    val isVisible by remember { derivedStateOf { url.isNotBlank() } } //Icon-Clear
    var isError by remember { mutableStateOf(false) } //Textinput Fehler

    val clipboardManager = ContextCompat.getSystemService(
        context,
        ClipboardManager::class.java
    ) as ClipboardManager

    val py = Python.getInstance()
    val module = py.getModule("youtube")

    if(transcriptResult == "ungültiger Link") {
        isError = true
    }else{
        isError = false
    }

    Box() {
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = {navController.navigate("settings")},
                    modifier = modifier.padding(start = 8.dp, top = 40.dp)
                ) {
                    Icon(Icons.Outlined.Settings, contentDescription = "Localized description")
                }
                IconButton(
                    onClick = {navController.navigate("history")},
                    modifier = modifier.padding(end = 8.dp, top = 40.dp)
                ) {
                    Icon( painter = painterResource(id = com.example.summaryyoupython.R.drawable.outline_library_books_24), contentDescription = "Localized description")
                }
            }
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(start = 20.dp, end = 20.dp)
            ) {
                Text(
                    text = "Summary You",
                    style = MaterialTheme.typography.headlineLarge
                )
                // Anzeige des Ergebnisses oder Ladeanzeige
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
                                text = "ungültiger Link",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    trailingIcon = {
                        if (isVisible) {
                            IconButton(
                                onClick = {
                                    url = ""
                                    transcriptResult = null
                                    focusRequester.requestFocus()
                                }
                            ) {
                                Icon(
                                    painter = painterResource(id = com.example.summaryyoupython.R.drawable.outline_cancel_24),
                                    contentDescription = "Cancel"
                                )
                            }
                        }
                    },
                    singleLine = true,
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
                                shape = SegmentedButtonDefaults.shape(position = index, count = options.size),
                                onClick = { selectedIndex = index },
                                selected = index == selectedIndex
                            ) {
                                Text(label)
                            }
                        }
                    }
                }
                if (!transcriptResult.isNullOrEmpty() && transcriptResult != "ungültiger Link") {
                    Card(
                        modifier = modifier
                            .padding(top = 15.dp, bottom = 15.dp)
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {},
                                onLongClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    // Inhalt der Karte in die Zwischenablage kopieren
                                    clipboardManager.setPrimaryClip(
                                        ClipData.newPlainText("Transkript", transcriptResult)
                                    )
                                }
                            )
                    ) {
                        if(transcriptResult!="ungültiger Link") {
                            if(!title.isNullOrEmpty()) {
                                Text(
                                    text = title ?: "",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = modifier
                                        .padding(top = 12.dp, start = 12.dp, end = 12.dp)
                                )
                                if(!author.isNullOrEmpty()) {
                                    Row {
                                        Text(
                                            text = author ?: "",
                                            style = MaterialTheme.typography.bodyLarge,
                                            modifier = modifier
                                                .padding(top = 4.dp, start = 12.dp, end = 12.dp)
                                        )
                                        if (isYouTubeLink(url)) {
                                            Icon(
                                                painter = painterResource(id = com.example.summaryyoupython.R.drawable.youtube),
                                                contentDescription = null,
                                                modifier = Modifier.padding(top = 1.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        Text(
                            text = transcriptResult ?: "Transkript nicht gefunden",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = modifier
                                .padding(start=12.dp, end=12.dp, top=10.dp, bottom=12.dp)
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
                                isLoading = true // Starte den Abruf
                                scope.launch {
                                    title = getTitel(url)
                                    author = getAuthor(url)
                                    transcriptResult = summarize(url, selectedIndex)
                                    isLoading = false // Setze isLoading auf false, wenn der Abruf abgeschlossen ist
                                    viewModel.addTextSummary(title, author, transcriptResult)
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
                            Text("Regenerate")
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
                        // Überprüfe, ob etwas in der Zwischenablage ist
                        val clipData = clipboardManager.primaryClip
                        if (clipData != null && clipData.itemCount > 0) {
                            val clipItem = clipData.getItemAt(0)
                            url = clipItem.text.toString()
                        }
                    }
                },
                modifier = modifier.padding(bottom = 20.dp, end = 15.dp)
            ) {
                Icon(painter = painterResource(id = com.example.summaryyoupython.R.drawable.outline_content_paste_24), "Localized description")
            }
            FloatingActionButton(
                onClick = {
                    focusManager.clearFocus()
                    isLoading = true // Starte den Abruf
                    scope.launch {
                        title = getTitel(url)
                        author = getAuthor(url)
                        transcriptResult = summarize(url, selectedIndex)
                        isLoading = false // Setze isLoading auf false, wenn der Abruf abgeschlossen ist
                        viewModel.addTextSummary(title, author, transcriptResult)
                    }
                },
                modifier = modifier.padding(bottom = 60.dp, end = 15.dp)
            ) {
                Icon(Icons.Filled.Check, "Localized description")
            }
        }
    }
}

suspend fun summarize(url: String, length: Int): String {
    val py = Python.getInstance()
    val module = py.getModule("youtube")
    val dotenv = dotenv {
        directory = "/assets"
        filename = "env" // instead of '.env', use 'env'
    }
    val key = dotenv["OPEN_AI_KEY"]

    try {
        val result = withContext(Dispatchers.IO) {
            module.callAttr("summarize", url, key, length).toString()
        }
        return result
    } catch (e: Exception) {
        // Fehlerbehandlung
        return "Fehler beim Abrufen der Zusammenfassung ${e.message}"
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
        // Fehlerbehandlung
        //return "Fehler beim Abrufen des Authors"
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
        // Fehlerbehandlung
        //return "Fehler beim Abrufen des Titels"
        return null
    }
}

fun isYouTubeLink(input: String): Boolean {
    val youtubePattern = Regex("""^(https?://)?(www\.)?(youtube\.com|youtu\.be)/.*$""")
    return youtubePattern.matches(input)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Textbox(modifier: Modifier = Modifier, title: String?, author: String?, text: String?, viewModel: TextSummaryViewModel) {
    val haptics = LocalHapticFeedback.current //Vibration bei kopieren von Zusammenfassung

    Card(
        modifier = modifier
            .padding(top = 15.dp, bottom = 15.dp)
            .fillMaxWidth()
            .combinedClickable(
                onClick = {},
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.removeTextSummary(title, author, text)
                }
            )
    ) {
        if(!title.isNullOrEmpty()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = modifier
                    .padding(top = 12.dp, start = 12.dp, end = 12.dp)
            )
            if (!author.isNullOrEmpty()) {
                Text(
                    text = author,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = modifier
                        .padding(top = 4.dp, start = 12.dp, end = 12.dp)
                )
            }
        }
        Text(
            text = text ?: "",
            style = MaterialTheme.typography.labelLarge,
            modifier = modifier
                .padding(12.dp)
        )
    }
}

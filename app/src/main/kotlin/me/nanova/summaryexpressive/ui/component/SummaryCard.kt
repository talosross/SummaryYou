package me.nanova.summaryexpressive.ui.component

import android.content.ClipData
import android.content.Intent
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.PauseCircleFilled
import androidx.compose.material.icons.outlined.PlayCircleFilled
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import kotlinx.coroutines.launch
import me.nanova.summaryexpressive.R
import me.nanova.summaryexpressive.llm.SummaryLength
import me.nanova.summaryexpressive.llm.SummaryOutput
import java.util.UUID

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SummaryCard(
    modifier: Modifier = Modifier,
    summary: SummaryOutput,
    cardColors: CardColors = CardDefaults.cardColors(),
    onLongClick: (() -> Unit)? = null,
    onShowSnackbar: (String) -> Unit,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {},
                onLongClick = onLongClick
            ),
        colors = cardColors,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp),
    ) {
        if (summary.title.isNotEmpty()) {
            Text(
                text = summary.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(top = 12.dp, start = 12.dp, end = 12.dp)
            )
            if (summary.author.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = summary.author,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    if (summary.isYoutubeLink) {
                        Icon(
                            painter = painterResource(id = R.drawable.youtube),
                            contentDescription = "Youtube Icon",
                            modifier = Modifier.padding(top = 1.dp)
                        )
                    }
                }
            }
        }
        Text(
            text = summary.summary,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .padding(
                    start = 12.dp,
                    end = 12.dp,
                    top = 10.dp,
                    bottom = 12.dp
                )
        )
        SummaryActionButtons(summary = summary, onShowSnackbar = onShowSnackbar)
    }
}

@Preview
@Composable
fun SummaryCardPreview() {
    val summary = SummaryOutput(
        title = "Sample Title",
        author = "Sample Author",
        summary = "This is a sample summary for preview purposes. It should be long enough to test the TTS functionality and also the layout of the card.",
        isYoutubeLink = true,
        length = SummaryLength.SHORT
    )
    SummaryCard(
        summary = summary,
        onShowSnackbar = {}
    )
}


@Composable
private fun SummaryActionButtons(
    summary: SummaryOutput,
    onShowSnackbar: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboard.current
    val summaryText = summary.summary

    var tts: TextToSpeech? by remember { mutableStateOf(null) }
    var isSpeaking by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableIntStateOf(0) }
    var utteranceId by remember { mutableStateOf("") }
    val copied = stringResource(id = R.string.copied)

    val utteranceProgressListener =
        remember {
            object : UtteranceProgressListener() {
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
                    onShowSnackbar("Failed to play")
                }

                override fun onRangeStart(
                    utteranceId: String,
                    start: Int,
                    end: Int,
                    frame: Int,
                ) {
                    // Is called when a new range of text is being spoken
                    currentPosition = end
                }
            }
        }

    DisposableEffect(Unit) {
        var textToSpeech: TextToSpeech? = null
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                Log.d("TTS", "TTS engine initialized.")
                textToSpeech?.setOnUtteranceProgressListener(utteranceProgressListener)
            } else {
                Log.d("TTS", "TTS engine init error.")
            }
        }
        tts = textToSpeech
        onDispose {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            onClick = {
                if (isSpeaking) {
                    tts?.stop()
                    isSpeaking = false
                    isPaused = false
                    currentPosition = 0
                } else if (summaryText.isNotEmpty()) {
                    utteranceId = UUID.randomUUID().toString()
                    tts?.speak(
                        summaryText,
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        utteranceId
                    )
                    isSpeaking = true
                }
            }
        ) {
            Icon(
                Icons.AutoMirrored.Outlined.VolumeUp,
                contentDescription = if (isSpeaking) "Finish" else "Read",
                modifier = Modifier.size(24.dp)
            )
        }

        AnimatedVisibility(visible = isSpeaking) {
            IconButton(
                onClick = {
                    if (isPaused) {
                        val remainingText =
                            summaryText.substring(currentPosition)
                        utteranceId =
                            UUID.randomUUID().toString()
                        tts?.speak(
                            remainingText,
                            TextToSpeech.QUEUE_FLUSH,
                            null,
                            utteranceId
                        )
                        isPaused = false
                    } else {
                        tts?.stop()
                        isPaused = true
                    }
                }
            ) {
                Icon(
                    if (isPaused) Icons.Outlined.PlayCircleFilled else Icons.Outlined.PauseCircleFilled,
                    contentDescription = if (isPaused) "Continue" else "Pause",
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        summary.sourceLink?.let {
            if (it.isNotEmpty()) {
                IconButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, it.toUri())
                        context.startActivity(intent)
                    }
                ) {
                    Icon(
                        Icons.Rounded.Link,
                        contentDescription = "Original Link",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        IconButton(
            onClick = {
                scope.launch {
                    clipboard.setClipEntry(
                        ClipData.newPlainText(
                            "User Input",
                            summaryText
                        ).toClipEntry()
                    )
                    onShowSnackbar(copied)
                }
            }
        ) {
            Icon(
                Icons.Rounded.ContentCopy,
                contentDescription = "Copy",
                modifier = Modifier.size(24.dp)
            )
        }

        IconButton(
            onClick = {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(
                        Intent.EXTRA_TEXT,
                        summaryText
                    )
                }
                val chooserIntent =
                    Intent.createChooser(shareIntent, null)
                context.startActivity(chooserIntent)
            }
        ) {
            Icon(
                imageVector = Icons.Outlined.Share,
                contentDescription = "Share",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Preview
@Composable
fun SummaryActionButtonsPreview() {
    val summary = SummaryOutput(
        title = "Sample Title",
        author = "Sample Author",
        summary = "This is a sample summary for preview purposes. It should be long enough to test the TTS functionality and also the layout of the card.",
        sourceLink = "https://xxx.yyy",
        isYoutubeLink = true,
        length = SummaryLength.SHORT
    )
    SummaryActionButtons(summary = summary, onShowSnackbar = {})
}
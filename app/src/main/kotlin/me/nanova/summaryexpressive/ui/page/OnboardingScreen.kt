package me.nanova.summaryexpressive.ui.page

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import coil.size.Size
import me.nanova.summaryexpressive.R

@Composable
fun OnboardingScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    var screen by remember { mutableIntStateOf(1) }
    val context = LocalContext.current
    val imageLoader = ImageLoader.Builder(context)
        .components {
            add(ImageDecoderDecoder.Factory())
        }
        .build()

    LazyColumn(
        modifier = modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            when (screen) {
                1 -> {
                    Image(
                        painter = painterResource(id = R.drawable.logo),
                        contentDescription = "Logo",
                        modifier = Modifier
                            .size(width = 150.dp, height = 150.dp)
                    )
                    Text(
                        text = stringResource(id = R.string.welcome),
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp,
                        modifier = Modifier.padding(top = 20.dp)
                    )
                    Text(
                        text = stringResource(id = R.string.welcomeDescription),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 12.dp, top = 15.dp)
                    )
                    Row {
                        FilledTonalButton(
                            modifier = Modifier
                                .padding(vertical = 18.dp),
                            onClick = onDone
                        ) {
                            Text(text = stringResource(id = R.string.skip))
                        }
                        Spacer(modifier = modifier.padding(start = 12.dp))
                        Button(
                            modifier = Modifier
                                .padding(vertical = 18.dp),
                            onClick = { screen = 2 }
                        ) {
                            Text(text = stringResource(id = R.string.continueButton))
                        }
                    }
                }

                2 -> {
                    Image(
                        painter = rememberAsyncImagePainter(
                            ImageRequest.Builder(context)
                                .data(data = R.drawable.screen1)
                                .apply(block = {
                                    size(Size.ORIGINAL)
                                })
                                .build(), imageLoader = imageLoader
                        ),
                        contentDescription = null,
                        modifier = modifier
                            .padding(top = 60.dp, start = 40.dp, end = 40.dp, bottom = 0.dp)
                            .clip(shape = RoundedCornerShape(20.dp)),
                    )
                    Text(
                        text = stringResource(id = R.string.instruction),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 12.dp, top = 18.dp)
                    )
                    Button(
                        modifier = Modifier.padding(vertical = 18.dp),
                        onClick = { screen = 3 }
                    ) {
                        Text(text = stringResource(id = R.string.continueButton))
                    }
                    Spacer(modifier = modifier.padding(top = 18.dp))
                }

                3 -> {
                    Image(
                        painter = rememberAsyncImagePainter(
                            ImageRequest.Builder(context)
                                .data(data = R.drawable.screen2)
                                .apply(block = {
                                    size(Size.ORIGINAL)
                                })
                                .build(), imageLoader = imageLoader
                        ),
                        contentDescription = null,
                        modifier = modifier
                            .padding(top = 60.dp, start = 40.dp, end = 40.dp, bottom = 0.dp)
                            .clip(shape = RoundedCornerShape(20.dp)),
                    )
                    Text(
                        text = stringResource(id = R.string.instructionsShare),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 12.dp, top = 15.dp)
                    )
                    Button(
                        modifier = Modifier.padding(vertical = 18.dp),
                        onClick = { screen = 4 }
                    ) {
                        Text(text = stringResource(id = R.string.continueButton))
                    }
                    Spacer(modifier = modifier.padding(top = 18.dp))
                }

                4 -> {
                    Image(
                        painter = rememberAsyncImagePainter(
                            ImageRequest.Builder(context)
                                .data(data = R.drawable.screen3)
                                .apply(block = {
                                    size(Size.ORIGINAL)
                                })
                                .build(), imageLoader = imageLoader
                        ),
                        contentDescription = null,
                        modifier = modifier
                            .padding(top = 60.dp, start = 40.dp, end = 40.dp, bottom = 0.dp)
                            .clip(shape = RoundedCornerShape(20.dp)),
                    )
                    Text(
                        text = stringResource(id = R.string.instructionsHistory),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 12.dp, top = 15.dp)
                    )
                    Button(
                        modifier = Modifier
                            .padding(vertical = 18.dp),
                        onClick = onDone
                    ) {
                        Text(text = stringResource(id = R.string.finishButton))
                    }
                    Spacer(modifier = modifier.padding(top = 18.dp))
                }
            }
        }
    }
}
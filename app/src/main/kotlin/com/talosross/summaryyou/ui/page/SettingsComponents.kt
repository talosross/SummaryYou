package com.talosross.summaryyou.ui.page

import androidx.compose.animation.Animatable
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.TweenSpec
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.talosross.summaryyou.R
import com.talosross.summaryyou.llm.AIProvider
import com.talosross.summaryyou.ui.component.ClickablePasteIcon
import com.talosross.summaryyou.ui.theme.SummaryYouTheme
import kotlinx.coroutines.launch

@Composable
internal fun SettingsGroup(
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    content: @Composable () -> Unit,
) {
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val secondaryContainerColor = MaterialTheme.colorScheme.secondaryContainer
    val animatedColor = remember(surfaceVariantColor) { Animatable(surfaceVariantColor) }
    val animatedBorderWidth = remember { Animatable(0f) }

    LaunchedEffect(highlighted, surfaceVariantColor, secondaryContainerColor) {
        if (highlighted) {
            launch {
                animatedColor.animateTo(
                    secondaryContainerColor,
                    animationSpec = TweenSpec(durationMillis = 600)
                )
                animatedColor.animateTo(
                    surfaceVariantColor,
                    animationSpec = TweenSpec(durationMillis = 1200)
                )
            }
            launch {
                animatedBorderWidth.animateTo(
                    3f,
                    animationSpec = TweenSpec(durationMillis = 600)
                )
                animatedBorderWidth.animateTo(
                    0f,
                    animationSpec = TweenSpec(durationMillis = 1200)
                )
            }
        } else {
            animatedColor.snapTo(surfaceVariantColor)
            animatedBorderWidth.snapTo(0f)
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = animatedColor.value),
        border = if (animatedBorderWidth.value > 0) BorderStroke(
            animatedBorderWidth.value.dp,
            MaterialTheme.colorScheme.secondary
        ) else null
    ) {
        content()
    }
}

@Composable
internal fun ThemeSettingsDialog(
    onDismissRequest: () -> Unit,
    currentTheme: Int,
    onThemeChange: (Int) -> Unit,
) {
    var theme by remember { mutableIntStateOf(currentTheme) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(id = R.string.theme)) },
        text = {
            Column {
                RadioButtonItem(selected = theme == 0, onSelectionChange = { theme = 0 }) {
                    Text(stringResource(id = R.string.systemTheme), style = MaterialTheme.typography.bodyLarge)
                }
                RadioButtonItem(selected = theme == 2, onSelectionChange = { theme = 2 }) {
                    Text(stringResource(id = R.string.lightTheme), style = MaterialTheme.typography.bodyLarge)
                }
                RadioButtonItem(selected = theme == 1, onSelectionChange = { theme = 1 }) {
                    Text(stringResource(id = R.string.darkTheme), style = MaterialTheme.typography.bodyLarge)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onThemeChange(theme); onDismissRequest() }) {
                Text(stringResource(id = R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text(stringResource(id = R.string.cancel)) }
        },
    )
}

@Composable
internal fun AIProviderSettingsDialog(
    onDismissRequest: () -> Unit,
    initialProvider: AIProvider,
    initialBaseUrl: String?,
    initialApiKey: String?,
    hasProxy: Boolean = false,
    onConfirm: (provider: AIProvider, baseUrl: String, apiKey: String) -> Unit,
    onNext: (provider: AIProvider, baseUrl: String, apiKey: String) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val apiKeyFocusRequester = remember { FocusRequester() }
    var selectedProvider by remember { mutableStateOf(initialProvider) }
    var baseUrlTextFieldValue by remember { mutableStateOf(initialBaseUrl ?: "") }
    var apiKeyTextFieldValue by remember { mutableStateOf(initialApiKey ?: "") }
    val isIntegrated = selectedProvider == AIProvider.INTEGRATED
    val formValid = isIntegrated
            || (selectedProvider.isMandatoryBaseUrl && baseUrlTextFieldValue.isNotBlank())
            || (selectedProvider.isRequiredApiKey && apiKeyTextFieldValue.isNotBlank())
            || hasProxy
    val providerChanged = selectedProvider != initialProvider
    val submit = {
        if (formValid) {
            if (providerChanged) {
                onNext(selectedProvider, baseUrlTextFieldValue, apiKeyTextFieldValue)
            } else {
                onConfirm(selectedProvider, baseUrlTextFieldValue, apiKeyTextFieldValue)
                onDismissRequest()
            }
        }
    }
    val availableProviders = AIProvider.entries.filter { it != AIProvider.INTEGRATED || hasProxy }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(id = R.string.setAIProvider)) },
        text = {
            Column {
                availableProviders.map {
                    AIProviderItem(it, selected = (selectedProvider == it)) { selectedProvider = it }
                }
                Spacer(modifier = Modifier.height(9.dp))
                OutlinedTextField(
                    value = baseUrlTextFieldValue,
                    onValueChange = { baseUrlTextFieldValue = it },
                    enabled = !isIntegrated,
                    label = { Text(if (selectedProvider.isMandatoryBaseUrl) "* Base Url" else "Custom URL") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.large,
                    leadingIcon = { Icon(Icons.Rounded.Link, contentDescription = "Base Url") },
                    trailingIcon = {
                        if (!isIntegrated) {
                            ClickablePasteIcon(
                                text = baseUrlTextFieldValue,
                                onPaste = { baseUrlTextFieldValue = it.trim() },
                                onClear = { baseUrlTextFieldValue = "" }
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { apiKeyFocusRequester.requestFocus() }),
                )
                Spacer(modifier = Modifier.height(9.dp))
                if (selectedProvider.isRequiredApiKey || isIntegrated) {
                    OutlinedTextField(
                        modifier = Modifier.focusRequester(apiKeyFocusRequester),
                        value = apiKeyTextFieldValue,
                        onValueChange = { apiKeyTextFieldValue = it },
                        enabled = !isIntegrated,
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Rounded.VpnKey, contentDescription = "API Key") },
                        label = {
                            Text(
                                if (isIntegrated) stringResource(R.string.setApiKey)
                                else if (hasProxy) stringResource(R.string.setApiKey) + " (optional)"
                                else "* " + stringResource(R.string.setApiKey)
                            )
                        },
                        shape = MaterialTheme.shapes.large,
                        trailingIcon = {
                            if (!isIntegrated) {
                                ClickablePasteIcon(
                                    text = apiKeyTextFieldValue,
                                    onPaste = { apiKeyTextFieldValue = it.trim() },
                                    onClear = { apiKeyTextFieldValue = "" }
                                )
                            }
                        },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            submit()
                        }),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(enabled = formValid, onClick = { submit() }) {
                Text(stringResource(id = if (providerChanged) R.string.next else R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text(stringResource(id = R.string.cancel)) }
        },
    )
}

@Composable
internal fun ModelSettingsDialog(
    onDismissRequest: () -> Unit,
    provider: AIProvider,
    initialModelId: String?,
    onConfirm: (modelId: String) -> Unit,
) {
    val customModelKey = "##CUSTOM_MODEL##"
    val isInitialModelCustom = initialModelId?.let { id -> provider.models.none { it.id == id } } == true
    var selectedKey by remember { mutableStateOf(if (isInitialModelCustom) customModelKey else initialModelId) }
    var customModelName by remember { mutableStateOf(if (isInitialModelCustom) initialModelId else "") }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            val providerDisplay = if (provider == AIProvider.INTEGRATED) stringResource(R.string.integratedProvider) else provider.id.display
            Text(stringResource(id = R.string.setModel) + " ($providerDisplay)")
        },
        text = {
            Column(Modifier.heightIn(max = 390.dp)) {
                LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                    items(provider.models.size) { index ->
                        val model = provider.models[index]
                        RadioButtonItem(selected = selectedKey == model.id, onSelectionChange = { selectedKey = model.id }) {
                            Text(text = model.id)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                RadioButtonItem(selected = selectedKey == customModelKey, onSelectionChange = { selectedKey = customModelKey }) {
                    OutlinedTextField(
                        value = customModelName,
                        onValueChange = { customModelName = it; selectedKey = customModelKey },
                        label = { Text("Custom Model") },
                        shape = MaterialTheme.shapes.large,
                        singleLine = true,
                        trailingIcon = {
                            ClickablePasteIcon(
                                text = customModelName,
                                onPaste = { customModelName = it.trim(); selectedKey = customModelKey },
                                onClear = { customModelName = "" }
                            )
                        },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val resultModelId = if (selectedKey == customModelKey) customModelName else selectedKey ?: ""
                    onConfirm(resultModelId)
                    onDismissRequest()
                },
                enabled = (selectedKey != customModelKey && selectedKey != null) || (selectedKey == customModelKey && customModelName.isNotBlank())
            ) { Text(stringResource(id = R.string.ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text(stringResource(id = R.string.cancel)) }
        },
    )
}

@Composable
internal fun RadioButtonItem(
    selected: Boolean,
    onSelectionChange: () -> Unit,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelectionChange, role = Role.RadioButton)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(modifier = Modifier.width(8.dp))
        content()
    }
}

@Composable
internal fun AIProviderItem(
    llm: AIProvider,
    selected: Boolean,
    onSelectionChange: () -> Unit,
) {
    RadioButtonItem(selected, onSelectionChange) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = ImageVector.vectorResource(id = llm.icon),
                tint = if (selected && !llm.isMonochromeIcon) Color.Unspecified else LocalContentColor.current,
                contentDescription = "${llm.id.display} icon",
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (llm == AIProvider.INTEGRATED) stringResource(R.string.integratedProvider) else llm.id.display,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Preview
@Composable
private fun AIProviderSettingsDialogPreview() {
    SummaryYouTheme {
        AIProviderSettingsDialog(
            onDismissRequest = {}, initialProvider = AIProvider.OPENAI,
            initialBaseUrl = "https://example.com", initialApiKey = "test_api_key",
            onConfirm = { _, _, _ -> }, onNext = { _, _, _ -> },
        )
    }
}

@Preview
@Composable
private fun ModelSettingsDialogPreview() {
    SummaryYouTheme {
        ModelSettingsDialog(
            onDismissRequest = {}, provider = AIProvider.OPENAI,
            onConfirm = { _ -> }, initialModelId = AIProvider.OPENAI.models.first().id,
        )
    }
}

@Preview
@Composable
private fun RadioButtonItemPreview() {
    SummaryYouTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            RadioButtonItem(selected = true, onSelectionChange = {}) { Text("Option 1") }
            RadioButtonItem(selected = false, onSelectionChange = {}) { Text("Option 2") }
        }
    }
}

@Preview
@Composable
private fun AIProviderItemPreview() {
    SummaryYouTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            AIProvider.entries.map { AIProviderItem(it, selected = it == AIProvider.GEMINI) {} }
        }
    }
}


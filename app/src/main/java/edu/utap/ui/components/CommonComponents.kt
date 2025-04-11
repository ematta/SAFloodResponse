package edu.utap.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Displays a centered circular progress indicator filling the available space.
 *
 * @param modifier Modifier to apply to the container.
 */
@Preview
@Composable
fun LoadingIndicator(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
/**
 * Styled outlined text field used throughout the app.
 *
 * @param value The current text value.
 * @param onValueChange Callback when the text changes.
 * @param label The label to display inside the text field.
 * @param modifier Modifier to apply to the text field.
 * @param enabled Whether the text field is enabled.
 * @param isError Whether to show error styling.
 * @param visualTransformation Visual transformation for the input (e.g., password).
 * @param supportingText Optional supporting/error text below the field.
 * @param keyboardOptions Keyboard options such as input type.
 */
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    supportingText: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        isError = isError,
        visualTransformation = visualTransformation,
        supportingText = supportingText?.let { { Text(it) } },
        keyboardOptions = keyboardOptions,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
/**
 * Styled button with optional loading indicator.
 *
 * @param onClick Callback when the button is clicked.
 * @param text The button label.
 * @param modifier Modifier to apply to the button.
 */
@Composable
fun AppButton(onClick: () -> Unit, text: String, modifier: Modifier = Modifier) {
    var loading by remember { mutableStateOf(false) }
    var enabled by remember { mutableStateOf(false) }

    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled && !loading
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            Text(text)
        }
    }
}

/**
 * Displays an error message styled with error color.
 *
 * @param message The error message text.
 * @param modifier Modifier to apply to the text.
 */
@Composable
fun ErrorMessage(message: String, modifier: Modifier = Modifier) {
    Text(
        text = message,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodyMedium,
        modifier = modifier
    )
}

/**
 * Spacer with fixed height.
 *
 * @param height Height in dp.
 */
@Composable
fun SpacerHeight(height: Int) {
    Spacer(modifier = Modifier.height(height.dp))
}

package com.example.doshka.ui.components


import androidx.compose.material3.*
import androidx.compose.runtime.Composable

@Composable
fun AddExecutorDialog(
    email: String,
    onEmailChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Додати виконавця") },
        text = {
            OutlinedTextField(
                value = email,
                onValueChange = onEmailChange,
                label = { Text("Email") },
                singleLine = true
            )
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Додати")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Скасувати")
            }
        }
    )
}
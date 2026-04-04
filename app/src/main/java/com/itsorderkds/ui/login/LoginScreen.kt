package com.itsorderkds.ui.login

// Pamiętaj o dodaniu tych importów na górze pliku

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.itsorderkds.R
import com.itsorderkds.ui.auth.AuthViewModel

@Composable
fun LoginScreen(viewModel: AuthViewModel) {

    val uiState by viewModel.uiState.collectAsState()
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    LazyColumn(                                // ① przewijany kontener
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp)
            .imePadding(),                    // ② reaguj na klawiaturę
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp) // odstępy pomiędzy itemami
    ) {

        /** ---------- 1. Logo i nagłówek ---------- **/
        item {
            Spacer(Modifier.height(32.dp))
            Image(
                painter = painterResource(R.drawable.logo),
                contentDescription = "Logo Aplikacji",
                modifier = Modifier.size(200.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text("Witaj z powrotem!", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Zaloguj się, aby kontynuować",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        /** ---------- 2. Pola tekstowe ---------- **/
        item {
            OutlinedTextField(
                value = uiState.domain,
                onValueChange = viewModel::onDomainChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Domena") },
                leadingIcon = { Icon(Icons.Default.Business, null) },
                singleLine = true,
                enabled = !uiState.isLoading,
            )
        }

        item {
            OutlinedTextField(
                value = uiState.email,
                onValueChange = viewModel::onEmailChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Email") },
                leadingIcon = { Icon(Icons.Default.Email, null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                enabled = !uiState.isLoading,
                isError = uiState.errorMessage != null
            )
        }

        item {
            OutlinedTextField(
                value = uiState.pass,
                onValueChange = viewModel::onPasswordChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Hasło") },
                leadingIcon = { Icon(Icons.Default.Lock, null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                enabled = !uiState.isLoading,
                isError = uiState.errorMessage != null,
                visualTransformation = if (passwordVisible) VisualTransformation.None
                else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Default.Visibility
                            else Icons.Default.VisibilityOff,
                            contentDescription = null
                        )
                    }
                }
            )
        }

        /** ---------- 3. Komunikat błędu ---------- **/
        if (uiState.errorMessage != null) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error)
                    Text(uiState.errorMessage ?: "", color = MaterialTheme.colorScheme.error)
                }
            }
        }

        /** ---------- 4. Przycisk logowania ---------- **/
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                contentAlignment = Alignment.Center
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator()
                } else {
                    Button(
                        onClick = viewModel::onLoginClick,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Zaloguj") }
                }
            }
        }

        /** ---------- 5. Link „Zapomniałeś hasła?” ---------- **/
        item {
            TextButton(onClick = { /* TODO */ }) { Text("Zapomniałeś hasła?") }
            Spacer(Modifier.height(8.dp))
        }
    }
}
package com.example.mindfocus.ui.feature.login

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mindfocus.R
import com.example.mindfocus.core.datastore.AuthPreferencesManager
import com.example.mindfocus.data.local.MindFocusDatabase
import com.example.mindfocus.data.repository.UserRepository

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val authPreferencesManager = remember { AuthPreferencesManager(context) }
    val database = remember { MindFocusDatabase.getInstance(context) }
    val userRepository = remember { UserRepository(database) }
    
    val viewModel: LoginViewModel = viewModel {
        LoginViewModel(context, authPreferencesManager, userRepository)
    }
    
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(uiState.isLoginSuccessful) {
        if (uiState.isLoginSuccessful) {
            onLoginSuccess()
        }
    }
    
    LoginContent(
        uiState = uiState,
        onUsernameChange = { viewModel.updateUsername(it) },
        onEmailChange = { viewModel.updateEmail(it) },
        onLoginClick = { viewModel.login { _ -> onLoginSuccess() } },
        onRegisterClick = { viewModel.toggleRegisterMode() },
        modifier = modifier
    )
}

@Composable
private fun LoginContent(
    uiState: LoginUiState,
    onUsernameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        colorResource(R.color.darkcharcoal),
                        colorResource(R.color.darkslategray)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(60.dp))
            
            // Logo/Title Section
            LoginHeader(isRegisterMode = uiState.isRegisterMode)
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Login Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colorResource(R.color.midnightblue).copy(alpha = 0.9f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Username Field
                    LoginTextField(
                        value = uiState.username,
                        onValueChange = onUsernameChange,
                        label = stringResource(R.string.username_hint),
                        icon = Icons.Outlined.Person,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // Email Field - only shown in register mode
                    if (uiState.isRegisterMode) {
                        LoginTextField(
                            value = uiState.email,
                            onValueChange = onEmailChange,
                            label = stringResource(R.string.email_hint),
                            icon = Icons.Outlined.Email,
                            keyboardType = KeyboardType.Email,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    // Error Message
                    if (uiState.errorMessage != null) {
                        Text(
                            text = uiState.errorMessage,
                            color = colorResource(R.color.coralred),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Login/Register Button
                    LoginButton(
                        onClick = onLoginClick,
                        isLoading = uiState.isLoading,
                        isRegisterMode = uiState.isRegisterMode,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Register/Login Link
                    RegisterLink(
                        onClick = onRegisterClick,
                        isRegisterMode = uiState.isRegisterMode,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun LoginHeader(isRegisterMode: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // App Logo/Icon Placeholder
        Card(
            modifier = Modifier.size(100.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = colorResource(R.color.skyblue).copy(alpha = 0.2f)
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🧠",
                    fontSize = 48.sp
                )
            }
        }
        
        Text(
            text = stringResource(R.string.app_name),
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = colorResource(R.color.amber),
            letterSpacing = 1.sp
        )
        
        Text(
            text = if (isRegisterMode) {
                "Create your account to get started"
            } else {
                stringResource(R.string.login_subtitle)
            },
            fontSize = 16.sp,
            color = colorResource(R.color.lightsteelblue),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun LoginTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = colorResource(R.color.lightsteelblue)) },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = colorResource(R.color.skyblue)
            )
        },
        modifier = modifier
            .clip(RoundedCornerShape(16.dp)),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = colorResource(R.color.skyblue),
            unfocusedBorderColor = colorResource(R.color.darkslateblue),
            focusedLabelColor = colorResource(R.color.skyblue),
            unfocusedLabelColor = colorResource(R.color.lightsteelblue),
            focusedContainerColor = colorResource(R.color.darkslateblue).copy(alpha = 0.5f),
            unfocusedContainerColor = colorResource(R.color.darkslateblue).copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp),
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            keyboardType = keyboardType
        ),
        singleLine = true
    )
}

@Composable
private fun LoginButton(
    onClick: () -> Unit,
    isLoading: Boolean,
    isRegisterMode: Boolean,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp)),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent
        ),
        contentPadding = PaddingValues(0.dp),
        enabled = !isLoading
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            colorResource(R.color.skyblue),
                            colorResource(R.color.amber)
                        )
                    )
                )
                .clip(RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = if (isRegisterMode) "Register" else stringResource(R.string.login_button),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun RegisterLink(
    onClick: () -> Unit,
    isRegisterMode: Boolean,
    modifier: Modifier = Modifier
) {
    TextButton(
        onClick = onClick,
        modifier = modifier
    ) {
        Text(
            text = if (isRegisterMode) {
                "Already have an account? Login"
            } else {
                stringResource(R.string.register_link)
            },
            color = colorResource(R.color.skyblue),
            fontSize = 14.sp
        )
    }
}



package com.example.mindfocus.ui.feature.login

import android.content.Context
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    val activity = context as? FragmentActivity
    val authPreferencesManager = remember { AuthPreferencesManager(context) }
    val database = remember { MindFocusDatabase.getInstance(context.applicationContext) }
    val userRepository = remember { UserRepository(database) }
    
    val viewModel: LoginViewModel = viewModel {
        LoginViewModel(context, authPreferencesManager, userRepository)
    }
    
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    
    LaunchedEffect(Unit) {
        viewModel.resetBiometricState()
    }
    
    LaunchedEffect(uiState.isLoginSuccessful) {
        if (uiState.isLoginSuccessful) {
            onLoginSuccess()
        }
    }
    
    if (uiState.showUserSelection && uiState.availableUsers.isNotEmpty()) {
        com.example.mindfocus.ui.feature.login.UserSelectionDialog(
            users = uiState.availableUsers,
            selectedUserId = uiState.selectedUserId,
            onUserSelected = { userId ->
                activity?.let {
                    viewModel.authenticateWithSelectedUser(userId, it) { _ -> onLoginSuccess() }
                }
            },
            onDismiss = {
                viewModel.dismissUserSelection()
            }
        )
    }
    
    LoginContent(
        uiState = uiState,
        onUsernameChange = { viewModel.updateUsername(it) },
        onEmailChange = { viewModel.updateEmail(it) },
        onLoginClick = { viewModel.login { _ -> onLoginSuccess() } },
        onRegisterClick = { viewModel.toggleRegisterMode() },
        onBiometricClick = {
            activity?.let {
                viewModel.authenticateWithBiometric(it) { _ -> onLoginSuccess() }
            }
        },
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
    onBiometricClick: () -> Unit,
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
            Spacer(modifier = Modifier.height(40.dp))
            
            LoginHeader(isRegisterMode = uiState.isRegisterMode)
            
            Spacer(modifier = Modifier.height(32.dp))
            
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
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    LoginTextField(
                        value = uiState.username,
                        onValueChange = onUsernameChange,
                        label = stringResource(R.string.username_hint),
                        icon = Icons.Outlined.Person,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
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
                    
                    if (uiState.errorMessage != null) {
                        Text(
                            text = uiState.errorMessage,
                            color = colorResource(R.color.coralred),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 4.dp)
                        )
                    }
                    
                    LoginButton(
                        onClick = onLoginClick,
                        isLoading = uiState.isLoading,
                        isRegisterMode = uiState.isRegisterMode,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    RegisterLink(
                        onClick = onRegisterClick,
                        isRegisterMode = uiState.isRegisterMode,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    if (!uiState.isRegisterMode && uiState.isBiometricAvailable) {
                        HorizontalDivider(
                            color = colorResource(R.color.lightsteelblue).copy(alpha = 0.3f),
                            thickness = 1.dp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        
                        SelectAccountButton(
                            onClick = onBiometricClick,
                            isLoading = uiState.isBiometricAuthenticating,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
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
                    text = stringResource(R.string.login_icon_brain),
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
                        stringResource(R.string.create_account_subtitle)
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
            .height(50.dp)
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
                    text = if (isRegisterMode) stringResource(R.string.register_button) else stringResource(R.string.login_button),
                    fontSize = 16.sp,
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
        modifier = modifier,
        contentPadding = PaddingValues(vertical = 4.dp, horizontal = 8.dp)
    ) {
        Text(
            text = if (isRegisterMode) {
                stringResource(R.string.already_have_account)
            } else {
                stringResource(R.string.register_link)
            },
            color = colorResource(R.color.skyblue),
            fontSize = 13.sp
        )
    }
}

@Composable
private fun BiometricButton(
    onClick: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp)),
        enabled = !isLoading,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = colorResource(R.color.skyblue)
        ),
        border = ButtonDefaults.outlinedButtonBorder(enabled = !isLoading).copy(
            brush = Brush.horizontalGradient(
                listOf(
                    colorResource(R.color.skyblue),
                    colorResource(R.color.amber)
                )
            ),
            width = 2.dp
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = colorResource(R.color.skyblue),
                strokeWidth = 2.dp
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = stringResource(R.string.biometric_auth_fingerprint_description),
                    tint = colorResource(R.color.skyblue)
                )
                Text(
                    text = stringResource(R.string.biometric_auth_button),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = colorResource(R.color.skyblue)
                )
            }
        }
    }
}

@Composable
private fun SelectAccountButton(
    onClick: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .height(50.dp)
            .clip(RoundedCornerShape(16.dp)),
        enabled = !isLoading,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = colorResource(R.color.skyblue)
        ),
        border = ButtonDefaults.outlinedButtonBorder(enabled = !isLoading).copy(
            brush = Brush.horizontalGradient(
                listOf(
                    colorResource(R.color.skyblue),
                    colorResource(R.color.amber)
                )
            ),
            width = 2.dp
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = colorResource(R.color.skyblue),
                strokeWidth = 2.dp
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = stringResource(R.string.biometric_auth_fingerprint_description),
                    tint = colorResource(R.color.skyblue)
                )
                Text(
                    text = stringResource(R.string.biometric_auth_select_account_button),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = colorResource(R.color.skyblue)
                )
            }
        }
    }
}



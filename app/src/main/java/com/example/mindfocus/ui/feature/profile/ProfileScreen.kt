package com.example.mindfocus.ui.feature.profile

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.mindfocus.R
import com.example.mindfocus.core.datastore.AuthPreferencesManager
import com.example.mindfocus.data.local.MindFocusDatabase
import com.example.mindfocus.data.repository.UserRepository

@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit = {},
    onAccountDeleted: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val authPreferencesManager = remember { AuthPreferencesManager(context) }
    val database = remember { MindFocusDatabase.getInstance(context.applicationContext) }
    val userRepository = remember { UserRepository(database) }
    val sessionRepository = remember { com.example.mindfocus.data.repository.SessionRepository(database) }
    
    val viewModel: ProfileViewModel = viewModel {
        ProfileViewModel(context, authPreferencesManager, userRepository, sessionRepository)
    }
    
    val uiState by viewModel.uiState.collectAsState()
    
    var showImageSourceDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    
    val hasCameraPermission = remember {
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }
    
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && imageUri != null) {
            imageUri?.let {
                viewModel.saveProfilePicture(it.toString())
            }
        }
    }
    
    //Function to take picture - defined before use
    val takePicture: () -> Unit = remember {
        {
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "profile_picture_${System.currentTimeMillis()}.jpg")
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, context.getString(R.string.profile_picture_folder))
                }
            }
            
            val uri = context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
            imageUri = uri
            uri?.let { takePictureLauncher.launch(it) }
        }
    }
    
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            takePicture()
        }
    }
    
    //Image Picker Launcher (from gallery)
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.saveProfilePicture(it.toString())
        }
    }
    
    fun handleImageClick() {
        showImageSourceDialog = true
    }
    
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            //animated Header
            AnimatedHeader(
                onNavigateBack = onNavigateBack,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = colorResource(R.color.amber)
                    )
                }
            } else {
                //animated Avatar/Sticker with Profile Picture
                AnimatedProfileAvatar(
                    profilePictureUri = uiState.profilePictureUri,
                    onImageClick = ::handleImageClick,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                //Image Source Dialog
                if (showImageSourceDialog) {
                    ImageSourceDialog(
                        onDismiss = { showImageSourceDialog = false },
                        onTakePhoto = {
                            showImageSourceDialog = false
                            if (hasCameraPermission) {
                                takePicture()
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        onChooseFromGallery = {
                            showImageSourceDialog = false
                            imagePickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    )
                }
                
                //Profile Info Card with animations
                AnimatedProfileCard(
                    uiState = uiState,
                    viewModel = viewModel,
                    onDeleteAccountClick = { showDeleteAccountDialog = true },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                //Statistics Cards
                ProfileStatisticsCards(
                    uiState = uiState,
                    modifier = Modifier.fillMaxWidth()
                )
                
                //Error Message
                uiState.errorMessage?.let { errorMessage ->
                    Spacer(modifier = Modifier.height(16.dp))
                    AnimatedMessageCard(
                        message = errorMessage,
                        isError = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                //Success Message
                uiState.successMessage?.let { successMessage ->
                    Spacer(modifier = Modifier.height(16.dp))
                    AnimatedMessageCard(
                        message = successMessage,
                        isError = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
        
        if (showDeleteAccountDialog) {
            DeleteAccountConfirmationDialog(
                onConfirm = {
                    showDeleteAccountDialog = false
                    viewModel.deleteAccount(onAccountDeleted)
                },
                onDismiss = { showDeleteAccountDialog = false }
            )
        }
    }
}

@Composable
private fun AnimatedProfileAvatar(
    profilePictureUri: String?,
    onImageClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "avatar_animation")
    
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale_animation"
    )
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rotation_animation"
    )
    
    //Fade in animation
    var visible by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "avatar_fade"
    )
    
    LaunchedEffect(Unit) {
        visible = true
    }
    
    Box(
        modifier = modifier
            .alpha(alpha),
        contentAlignment = Alignment.Center
    ) {
        //Animated gradient background circles with rotation
        Box(
            modifier = Modifier
                .size(120.dp)
                .scale(scale)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            colorResource(R.color.amber).copy(alpha = 0.4f),
                            colorResource(R.color.skyblue).copy(alpha = 0.2f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )
        
        //Main avatar circle with gradient or image
        Box(
            modifier = Modifier
                .size(100.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(
                    if (profilePictureUri == null) {
                        Brush.linearGradient(
                            colors = listOf(
                                colorResource(R.color.amber),
                                colorResource(R.color.skyblue)
                            )
                        )
                    } else {
                        Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Transparent
                            )
                        )
                    },
                    shape = CircleShape
                )
                .clickable(onClick = onImageClick),
            contentAlignment = Alignment.Center
        ) {
            if (profilePictureUri != null) {
                AsyncImage(
                    model = Uri.parse(profilePictureUri),
                    contentDescription = stringResource(R.string.profile_picture_description),
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = stringResource(R.string.profile_icon_user),
                    fontSize = 50.sp,
                    modifier = Modifier
                        .scale(1f + (scale - 1f) * 0.3f)
                )
            }
        }
        
        //Camera icon button overlay
        Box(
            modifier = Modifier
                .size(100.dp)
                .offset(x = 35.dp, y = 35.dp),
            contentAlignment = Alignment.Center
        ) {
            FloatingActionButton(
                onClick = onImageClick,
                modifier = Modifier.size(32.dp),
                containerColor = colorResource(R.color.amber),
                contentColor = Color.White
            ) {
                Icon(
                    imageVector = Icons.Outlined.CameraAlt,
                    contentDescription = stringResource(R.string.profile_change_picture_description),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        
    }
}

@Composable
private fun AnimatedProfileCard(
    uiState: ProfileUiState,
    viewModel: ProfileViewModel,
    onDeleteAccountClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    //Fade in animation
    var visible by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "fade_animation"
    )
    
    //Scale animation
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.9f,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "scale_animation"
    )
    
    LaunchedEffect(Unit) {
        visible = true
    }
    
    Card(
        modifier = modifier
            .alpha(alpha)
            .scale(scale),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorResource(R.color.midnightblue).copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            //Username Section
            AnimatedUsernameSection(
                uiState = uiState,
                viewModel = viewModel
            )
            
            HorizontalDivider(
                color = colorResource(R.color.lightsteelblue).copy(alpha = 0.3f),
                thickness = 1.dp
            )
            
            //Email Section
            AnimatedEmailSection(
                email = uiState.email
            )
            
            HorizontalDivider(
                color = colorResource(R.color.lightsteelblue).copy(alpha = 0.3f),
                thickness = 1.dp
            )
            
            DeleteAccountSection(
                onClick = onDeleteAccountClick
            )
        }
    }
}

@Composable
private fun AnimatedUsernameSection(
    uiState: ProfileUiState,
    viewModel: ProfileViewModel
) {
    var visible by remember { mutableStateOf(false) }
    val sectionAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(600, delayMillis = 200, easing = FastOutSlowInEasing),
        label = "section_fade"
    )
    
    val infiniteTransition = rememberInfiniteTransition(label = "icon_animation")
    val iconScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "icon_scale"
    )
    
    LaunchedEffect(Unit) {
        visible = true
    }
    
    Column(
        modifier = Modifier.alpha(sectionAlpha),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = null,
                    tint = colorResource(R.color.amber),
                    modifier = Modifier
                        .size(20.dp)
                        .scale(iconScale)
                )
                Text(
                    text = stringResource(R.string.profile_username_label),
                    fontSize = 12.sp,
                    color = colorResource(R.color.lightsteelblue),
                    fontWeight = FontWeight.Medium
                )
            }
            
            if (!uiState.isEditingUsername) {
                IconButton(
                    onClick = { viewModel.startEditingUsername() },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = stringResource(R.string.profile_edit_username),
                        tint = colorResource(R.color.amber),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        
        if (uiState.isEditingUsername) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = uiState.newUsername,
                    onValueChange = { viewModel.updateNewUsername(it) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colorResource(R.color.lightsteelblue),
                        unfocusedTextColor = colorResource(R.color.lightsteelblue),
                        focusedBorderColor = colorResource(R.color.amber),
                        unfocusedBorderColor = colorResource(R.color.lightsteelblue).copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.cancelEditingUsername() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = colorResource(R.color.lightsteelblue)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.profile_cancel))
                    }
                    
                    Button(
                        onClick = { viewModel.saveUsername() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorResource(R.color.amber)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.profile_save))
                    }
                }
            }
        } else {
            Text(
                text = uiState.username ?: stringResource(R.string.profile_username_not_available),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = colorResource(R.color.amber)
            )
        }
    }
}

@Composable
private fun AnimatedEmailSection(
    email: String?
) {
    var visible by remember { mutableStateOf(false) }
    val sectionAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(600, delayMillis = 400, easing = FastOutSlowInEasing),
        label = "email_section_fade"
    )
    
    val infiniteTransition = rememberInfiniteTransition(label = "email_icon_animation")
    val iconScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "email_icon_scale"
    )
    
    LaunchedEffect(Unit) {
        visible = true
    }
    
    Column(
        modifier = Modifier.alpha(sectionAlpha),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Email,
                contentDescription = null,
                tint = colorResource(R.color.amber),
                modifier = Modifier
                    .size(20.dp)
                    .scale(iconScale)
            )
            Text(
                text = stringResource(R.string.profile_email_label),
                fontSize = 12.sp,
                color = colorResource(R.color.lightsteelblue),
                fontWeight = FontWeight.Medium
            )
        }
        
        Text(
            text = email ?: stringResource(R.string.profile_email_not_available),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = colorResource(R.color.amber)
        )
    }
}

@Composable
private fun AnimatedHeader(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "header_fade"
    )
    
    val slideOffset by animateFloatAsState(
        targetValue = if (visible) 0f else -50f,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "header_slide"
    )
    
    val infiniteTransition = rememberInfiniteTransition(label = "title_glow")
    val titleScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "title_scale"
    )
    
    LaunchedEffect(Unit) {
        visible = true
    }
    
    Row(
        modifier = modifier
            .alpha(alpha)
            .offset(y = slideOffset.dp)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.scale(1f + (titleScale - 1f) * 0.3f)
            ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back_button),
                            tint = colorResource(R.color.amber)
                        )
            }
            Text(
                text = stringResource(R.string.profile_title),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = colorResource(R.color.amber),
                modifier = Modifier.scale(titleScale)
            )
        }
    }
}

@Composable
private fun AnimatedMessageCard(
    message: String,
    isError: Boolean,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "message_fade"
    )
    
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.95f,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "message_scale"
    )
    
    LaunchedEffect(Unit) {
        visible = true
    }
    
    Card(
        modifier = modifier
            .alpha(alpha)
            .scale(scale),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isError) {
                colorResource(R.color.coralred).copy(alpha = 0.2f)
            } else {
                colorResource(R.color.amber).copy(alpha = 0.2f)
            }
        )
    ) {
        Text(
            text = message,
            color = if (isError) colorResource(R.color.coralred) else colorResource(R.color.amber),
            fontSize = 14.sp,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
private fun ImageSourceDialog(
    onDismiss: () -> Unit,
    onTakePhoto: () -> Unit,
    onChooseFromGallery: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.profile_change_picture),
                fontWeight = FontWeight.Bold,
                color = colorResource(R.color.amber)
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(
                    onClick = onTakePhoto,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CameraAlt,
                            contentDescription = null,
                            tint = colorResource(R.color.amber)
                        )
                        Text(
                            text = stringResource(R.string.profile_take_photo),
                            color = colorResource(R.color.lightsteelblue)
                        )
                    }
                }
                
                TextButton(
                    onClick = onChooseFromGallery,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = null,
                            tint = colorResource(R.color.amber)
                        )
                        Text(
                            text = stringResource(R.string.profile_choose_from_gallery),
                            color = colorResource(R.color.lightsteelblue)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.profile_cancel),
                    color = colorResource(R.color.lightsteelblue)
                )
            }
        },
        containerColor = colorResource(R.color.midnightblue).copy(alpha = 0.95f),
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
private fun ProfileStatisticsCards(
    uiState: ProfileUiState,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(600, delayMillis = 300, easing = FastOutSlowInEasing),
        label = "stats_fade"
    )
    
    LaunchedEffect(Unit) {
        visible = true
    }
    
    Column(
        modifier = modifier.alpha(alpha),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        //Statistics Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatisticCard(
                icon = stringResource(R.string.profile_icon_total_sessions),
                label = stringResource(R.string.profile_total_sessions),
                value = uiState.totalSessions.toString(),
                modifier = Modifier.weight(1f)
            )
            
            StatisticCard(
                icon = stringResource(R.string.profile_icon_avg_focus),
                label = stringResource(R.string.profile_avg_focus),
                value = if (uiState.averageFocusScore != null) {
                    String.format("%.0f", uiState.averageFocusScore)
                } else {
                    "--"
                },
                modifier = Modifier.weight(1f)
            )
        }
        
        //Time and Account Info Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatisticCard(
                icon = stringResource(R.string.profile_icon_total_time),
                label = stringResource(R.string.profile_total_time),
                value = formatTime(uiState.totalSessionTime),
                modifier = Modifier.weight(1f)
            )
            
            if (uiState.accountCreatedDate != null) {
                StatisticCard(
                    icon = stringResource(R.string.profile_icon_member_since),
                    label = stringResource(R.string.profile_member_since),
                    value = uiState.accountCreatedDate,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StatisticCard(
    icon: String,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.8f,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "stat_card_scale"
    )
    
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(100)
        visible = true
    }
    
    Card(
        modifier = modifier.scale(scale),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorResource(R.color.midnightblue).copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = icon,
                fontSize = 24.sp
            )
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = colorResource(R.color.amber)
            )
            Text(
                text = label,
                fontSize = 10.sp,
                color = colorResource(R.color.lightsteelblue).copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun formatTime(seconds: Long): String {
    return when {
        seconds < 60 -> "${seconds}s"
        seconds < 3600 -> {
            val minutes = seconds / 60
            val remainingSeconds = seconds % 60
            if (remainingSeconds > 0) {
                "${minutes}m ${remainingSeconds}s"
            } else {
                "${minutes}m"
            }
        }
        seconds < 86400 -> {
            val hours = seconds / 3600
            val remainingMinutes = (seconds % 3600) / 60
            if (remainingMinutes > 0) {
                "${hours}h ${remainingMinutes}m"
            } else {
                "${hours}h"
            }
        }
        else -> {
            val days = seconds / 86400
            val remainingHours = (seconds % 86400) / 3600
            if (remainingHours > 0) {
                "${days}d ${remainingHours}h"
            } else {
                "${days}d"
            }
        }
    }
}

@Composable
private fun DeleteAccountSection(
    onClick: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    val sectionAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(600, delayMillis = 600, easing = FastOutSlowInEasing),
        label = "delete_section_fade"
    )
    
    val infiniteTransition = rememberInfiniteTransition(label = "delete_icon_animation")
    val iconScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "delete_icon_scale"
    )
    
    LaunchedEffect(Unit) {
        visible = true
    }
    
    Column(
        modifier = Modifier.alpha(sectionAlpha),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = null,
                    tint = colorResource(R.color.coralred),
                    modifier = Modifier
                        .size(20.dp)
                        .scale(iconScale)
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = stringResource(R.string.profile_delete_account),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorResource(R.color.coralred)
                    )
                    Text(
                        text = stringResource(R.string.profile_delete_account_hint),
                        fontSize = 11.sp,
                        color = colorResource(R.color.lightsteelblue).copy(alpha = 0.7f)
                    )
                }
            }
            
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = colorResource(R.color.coralred).copy(alpha = 0.6f),
                modifier = Modifier
                    .size(20.dp)
                    .rotate(180f)
            )
        }
    }
}

@Composable
private fun DeleteAccountConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.profile_delete_account_confirmation_title),
                fontWeight = FontWeight.Bold,
                color = colorResource(R.color.coralred)
            )
        },
        text = {
            Text(
                text = stringResource(R.string.profile_delete_account_confirmation_message),
                color = colorResource(R.color.lightsteelblue)
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(R.color.coralred)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.profile_delete_account_confirm),
                    color = Color.White
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = colorResource(R.color.lightsteelblue)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.profile_delete_account_cancel))
            }
        },
        containerColor = colorResource(R.color.midnightblue).copy(alpha = 0.95f),
        shape = RoundedCornerShape(20.dp)
    )
}
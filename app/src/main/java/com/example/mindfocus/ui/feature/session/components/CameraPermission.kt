package com.example.mindfocus.ui.feature.session.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.mindfocus.R

@Composable
fun CameraPermission(onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.fillMaxSize()
    ) {
        Icon(
            Icons.Rounded.Warning,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(70.dp)
        )
        Text(
            stringResource(R.string.Permissions),
            color = Color.White,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )
        TextButton(onClick = onOpenSettings) {
            Text(stringResource(R.string.Settings), color = Color.White)
        }
    }
}


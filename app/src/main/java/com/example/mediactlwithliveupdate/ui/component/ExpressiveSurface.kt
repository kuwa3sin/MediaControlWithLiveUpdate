package com.kuwa3sin.mediactlwithliveupdate.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun ExpressiveSurface(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.extraLarge,
    tonalElevation: Dp = 6.dp,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.clip(shape),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        shape = shape,
        elevation = CardDefaults.cardElevation(defaultElevation = tonalElevation)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            content()
        }
    }
}
package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

enum class JarvisTab(val title: String, val icon: ImageVector) {
    VOICE("Voice", Icons.Default.Mic),
    CHAT("Chat", Icons.Default.ChatBubble),
    STATUS("Status", Icons.Default.Sensors),
    SMART("Smart", Icons.Default.Widgets),
    STUDY("Study", Icons.Default.MenuBook),
    PRODUCTIVITY("Tasks", Icons.Default.CheckCircle),
    UTILITIES("Tools", Icons.Default.Build),
    SETTINGS("Settings", Icons.Default.Settings)
}

@Composable
fun JarvisNavBar(
    selectedTab: JarvisTab,
    onTabSelected: (JarvisTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .glassCard(
                cornerRadius = 28.dp,
                backgroundColor = DeepSpaceSurface,
                borderColor = GlassBorderColor
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            JarvisTab.entries.forEach { tab ->
                val isSelected = tab == selectedTab
                val iconColor by animateColorAsState(
                    targetValue = if (isSelected) NeonCyan else TextSecondary,
                    label = "TabIconColor"
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onTabSelected(tab) }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) NeonCyan.copy(alpha = 0.18f)
                                else Color.Transparent
                            )
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.title,
                            tint = iconColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = tab.title,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) NeonCyan else TextSecondary
                    )
                }
            }
        }
    }
}


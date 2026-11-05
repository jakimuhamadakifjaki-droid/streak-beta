package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

enum class FlareTab(val title: String) {
    HOME("Home"),
    STREAK("Streak"),
    CHAT("Chat"),
    FRIENDS("Friends"),
    PROFILE("Profile")
}

@Composable
fun FlareBottomNav(
    currentTab: FlareTab,
    onTabSelected: (FlareTab) -> Unit,
    unreadChatCount: Int = 0,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(28.dp),
                ambientColor = FlareOrange.copy(alpha = 0.2f),
                spotColor = FlareOrange.copy(alpha = 0.3f)
            )
            .clip(RoundedCornerShape(28.dp))
            .background(Color(0xFF0F172A).copy(alpha = 0.95f))
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        FlareDarkCardBorder.copy(alpha = 0.8f),
                        FlareOrange.copy(alpha = 0.2f)
                    )
                ),
                shape = RoundedCornerShape(28.dp)
            )
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FlareTab.values().forEach { tab ->
                val isSelected = tab == currentTab

                val icon = when (tab) {
                    FlareTab.HOME -> if (isSelected) Icons.Filled.Home else Icons.Outlined.Home
                    FlareTab.STREAK -> if (isSelected) Icons.Filled.LocalFireDepartment else Icons.Outlined.LocalFireDepartment
                    FlareTab.CHAT -> if (isSelected) Icons.Filled.ChatBubble else Icons.Outlined.ChatBubbleOutline
                    FlareTab.FRIENDS -> if (isSelected) Icons.Filled.People else Icons.Outlined.PeopleOutline
                    FlareTab.PROFILE -> if (isSelected) Icons.Filled.Person else Icons.Outlined.PersonOutline
                }

                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.05f else 0.95f,
                    animationSpec = spring(),
                    label = "tab_scale"
                )

                val contentColor by animateColorAsState(
                    targetValue = if (isSelected) FlareOrange else FlareTextSecondary,
                    label = "tab_color"
                )

                Column(
                    modifier = Modifier
                        .scale(scale)
                        .clip(RoundedCornerShape(20.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onTabSelected(tab) }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    BadgedBox(
                        badge = {
                            if (tab == FlareTab.CHAT && unreadChatCount > 0) {
                                Badge(
                                    containerColor = FlareCrimson,
                                    contentColor = Color.White
                                ) {
                                    Text(text = "$unreadChatCount", fontSize = 10.sp)
                                }
                            }
                        }
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) FlareOrange.copy(alpha = 0.15f) else Color.Transparent
                                )
                                .padding(6.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = tab.title,
                                tint = contentColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = tab.title,
                        color = contentColor,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )

                    // Small indicator dot when selected
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(FlareOrange)
                        )
                    } else {
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }
    }
}

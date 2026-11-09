package corsica.erwinn.tracce.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import corsica.erwinn.tracce.ui.theme.Terracotta

data class NavigationTab(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector = icon
)

@Composable
fun AppBottomBar(
    currentRoute: String,
    isAdmin: Boolean,
    onTabSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = remember(isAdmin) {
        mutableListOf(
            NavigationTab(
                route = "profile",
                label = "Exploration",
                icon = Icons.Default.Map
            ),
            NavigationTab(
                route = "quests",
                label = "Quêtes",
                icon = Icons.Default.Explore
            ),
            NavigationTab(
                route = "settings",
                label = "Réglages",
                icon = Icons.Default.Settings
            )
        ).apply {
            if (isAdmin) {
                add(
                    NavigationTab(
                        route = "admin",
                        label = "Admin",
                        icon = Icons.Default.AdminPanelSettings
                    )
                )
            }
        }
    }

    // Barre flottante style Telegram (arrondie, ombre douce, fond sombre moderne)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFF1E2E25), // Vert nuit maquis profond
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEach { tab ->
                    val isSelected = currentRoute == tab.route

                    val pillColor by animateColorAsState(
                        targetValue = if (isSelected) Terracotta else Color.Transparent,
                        animationSpec = tween(220),
                        label = "pillColor"
                    )

                    val pillWidth by animateDpAsState(
                        targetValue = if (isSelected) 56.dp else 40.dp,
                        animationSpec = tween(220),
                        label = "pillWidth"
                    )

                    val contentColor by animateColorAsState(
                        targetValue = if (isSelected) Color.White else Color(0xFF9EABA2),
                        animationSpec = tween(220),
                        label = "contentColor"
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                onTabSelected(tab.route)
                            }
                            .padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Pilule d'icône sélectionnée comme sur Telegram
                        Box(
                            modifier = Modifier
                                .height(32.dp)
                                .width(pillWidth)
                                .clip(CircleShape)
                                .background(pillColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isSelected) tab.selectedIcon else tab.icon,
                                contentDescription = tab.label,
                                tint = contentColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(3.dp))

                        Text(
                            text = tab.label,
                            color = contentColor,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

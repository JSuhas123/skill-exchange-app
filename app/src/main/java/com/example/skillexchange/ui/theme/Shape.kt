package com.example.skillexchange.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Material 3 Shapes for SkillExchange
 * Uses modern rounded corners suitable for government and rural deployment
 */
val SkillExchangeShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),      // Subtle accents, small components
    small = RoundedCornerShape(8.dp),           // Input fields, small buttons
    medium = RoundedCornerShape(12.dp),         // Cards, dialogs
    large = RoundedCornerShape(16.dp),          // FAB, large buttons
    extraLarge = RoundedCornerShape(28.dp)      // Containers, bottom sheets
)

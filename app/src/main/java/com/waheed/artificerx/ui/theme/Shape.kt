package com.waheed.artificerx.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val ArtificerXShapes =
    Shapes(
        extraSmall = RoundedCornerShape(6.dp),
        small = RoundedCornerShape(10.dp),
        medium = RoundedCornerShape(16.dp),
        large = RoundedCornerShape(24.dp),
        extraLarge = RoundedCornerShape(32.dp),
    )

val GlassCardShape = RoundedCornerShape(20.dp)
val GlassPanelShape = RoundedCornerShape(28.dp)
val ChatBubbleUserShape =
    RoundedCornerShape(
        topStart = 20.dp,
        topEnd = 20.dp,
        bottomStart = 20.dp,
        bottomEnd = 4.dp,
    )
val ChatBubbleAgentShape =
    RoundedCornerShape(
        topStart = 20.dp,
        topEnd = 20.dp,
        bottomStart = 4.dp,
        bottomEnd = 20.dp,
    )
val ToolCallChipShape = RoundedCornerShape(50)
val LayerRowShape = RoundedCornerShape(12.dp)
val QualityBadgeShape = RoundedCornerShape(8.dp)
val BottomSheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
val FabShape = RoundedCornerShape(18.dp)

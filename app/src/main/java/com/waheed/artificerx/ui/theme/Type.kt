package com.waheed.artificerx.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val NotoSansFamily = FontFamily(Font(com.waheed.artificerx.R.font.noto_sans_regular))
val NotoMonoFamily = FontFamily(Font(com.waheed.artificerx.R.font.noto_sans_mono))
val DejaVuBoldFamily = FontFamily(Font(com.waheed.artificerx.R.font.deja_vu_sans_bold, FontWeight.Bold))

val ArtificerXTypography = Typography(
    displayLarge = TextStyle(fontFamily = DejaVuBoldFamily, fontSize = 36.sp, lineHeight = 42.sp, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontFamily = NotoSansFamily, fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontFamily = NotoSansFamily, fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontFamily = NotoSansFamily, fontSize = 16.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontFamily = NotoSansFamily, fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontFamily = NotoSansFamily, fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
)
val AgentLogTextStyle = TextStyle(fontFamily = NotoMonoFamily, fontSize = 12.sp, lineHeight = 18.sp)

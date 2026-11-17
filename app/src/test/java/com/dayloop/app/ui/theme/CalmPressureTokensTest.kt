package com.dayloop.app.ui.theme

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class CalmPressureTokensTest {
    @Test
    fun spacingTokensUseStableFoundationValues() {
        listOf(DayLoopLightTokens, DayLoopDarkTokens).forEach { tokens ->
            assertEquals(24.dp, tokens.spacing.pageMargin)
            assertEquals(24.dp, tokens.spacing.sectionGap)
            assertEquals(16.dp, tokens.spacing.cardPadding)
            assertEquals(12.dp, tokens.spacing.itemGap)
            assertEquals(8.dp, tokens.spacing.relatedGap)
        }
    }

    @Test
    fun lightAndDarkTokensKeepDistinctSupportingColors() {
        assertNotEquals(
            DayLoopLightTokens.colors.secondaryText,
            DayLoopDarkTokens.colors.secondaryText,
        )
    }
}

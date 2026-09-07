package com.dayloop.app.ui.theme

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class CalmPressureTokensTest {
    @Test
    fun spacingTokensUseStableFoundationValues() {
        assertEquals(24.dp, CalmPressureLightTokens.spacing.screenPadding)
        assertEquals(16.dp, CalmPressureLightTokens.spacing.contentGap)
        assertEquals(8.dp, CalmPressureLightTokens.spacing.compactGap)
    }

    @Test
    fun lightAndDarkTokensKeepDistinctSupportingColors() {
        assertNotEquals(
            CalmPressureLightTokens.colors.supportingText,
            CalmPressureDarkTokens.colors.supportingText,
        )
    }
}
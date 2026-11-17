package com.dayloop.app.feature.today

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.dayloop.app.core.database.OutfitEntity
import com.dayloop.app.core.database.OutfitWithItems
import com.dayloop.app.core.database.WardrobeMediaEntity
import com.dayloop.app.core.model.ClothingItem
import com.dayloop.app.core.model.ClothingItemId
import com.dayloop.app.core.model.ClothingCategory
import com.dayloop.app.ui.theme.DayLoopTheme
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalSharedTransitionApi::class)
class TodayLayoutRegressionTest {
    @get:Rule val compose = createComposeRule()
    private val date = LocalDate.of(2026, 9, 22)

    @Test fun photoOnlySavedOutfitHidesPromptAndClearingRestoresIt() {
        var saved by mutableStateOf<OutfitWithItems?>(OutfitWithItems(OutfitEntity(date.toString()), emptyList(),
            listOf(WardrobeMediaEntity("photo", "photo.jpg", "image/jpeg", 1, 1, 0L))))
        compose.setContent {
            DayLoopTheme { TodayScreen(TodayUiState.empty(date), todayOutfit = saved) }
        }
        compose.onNodeWithText("Today's Outfit").assertDoesNotExist()
        compose.runOnIdle { saved = null }
        compose.onNodeWithText("Today's Outfit").assertExists()
        compose.onNodeWithContentDescription("Import wardrobe details").assertExists()
        compose.onNodeWithText("Suggest").assertDoesNotExist()
    }

    @Test fun loadingDoesNotFlashAnUnrecordedOutfitPrompt() {
        compose.setContent {
            DayLoopTheme { TodayScreen(TodayUiState.empty(date), outfitsLoaded = false) }
        }
        compose.onNodeWithText("Today's Outfit").assertDoesNotExist()
    }

    @Test fun titleRemainsStationaryWhenListScrolls() {
        compose.setContent {
            DayLoopTheme { TodayScreen(TodayUiState.empty(date), wardrobeAvailable = true) }
        }
        val before = compose.onNodeWithTag("today-header").fetchSemanticsNode().boundsInRoot
        compose.onNodeWithTag("today-list").performTouchInput { swipeUp() }
        compose.onNodeWithTag("today-header").assertIsDisplayed()
        val after = compose.onNodeWithTag("today-header").fetchSemanticsNode().boundsInRoot
        assertEquals(before, after)
    }

    @Test fun suggestionStaysVisibleUntilSavedRecordArrives() {
        var wearClicks = 0
        val preview = listOf(ClothingItem(ClothingItemId("shirt"), "Shirt", ClothingCategory.TOP))
        compose.setContent {
            DayLoopTheme { TodayScreen(TodayUiState.empty(date), suggestionPreview = preview,
                wardrobeAvailable = true, onWearSuggestion = { wearClicks++ }) }
        }
        compose.onNodeWithText("Wear this").performScrollTo().performClick()
        compose.onNodeWithText("Suggested Outfit").assertExists()
        compose.runOnIdle { assertEquals(1, wearClicks) }
    }
}

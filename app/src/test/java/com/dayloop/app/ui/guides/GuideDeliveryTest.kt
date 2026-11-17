package com.dayloop.app.ui.guides

import com.dayloop.app.settings.GuideAction
import com.dayloop.app.settings.GuideProgress
import com.dayloop.app.settings.GuideStatus
import org.junit.Assert.*
import org.junit.Test

class GuideDeliveryTest {
    @Test fun nextAdvancesToDifferentStepAndLastCompletes() {
        for (key in listOf(GuideDefinitions.FirstLaunch, GuideDefinitions.Today)) {
            val steps = GuideDefinitions.getSteps(key)
            assertEquals(steps.size, steps.map { it.id }.distinct().size)
            steps.forEachIndexed { index, step ->
                assertEquals(steps.getOrNull(index + 1)?.let { GuideAction.Advance(it.id) }
                    ?: GuideAction.Complete, GuideDefinitions.nextAction(key, step.id))
            }
            assertNull(GuideDefinitions.nextAction(key, "missing"))
        }
    }

    @Test fun selectionWaitsForStorageAndRespectsExistingUsers() {
        val unseen = GuideProgress()
        assertNull(GuideDefinitions.select(null, unseen, true))
        assertEquals(GuideDefinitions.FirstLaunch, GuideDefinitions.select(unseen, unseen, true))
        assertEquals(GuideDefinitions.Today, GuideDefinitions.select(unseen, unseen, false))
        val resumed = unseen.apply(GuideAction.Start("orientation"))
        assertEquals(GuideDefinitions.FirstLaunch, GuideDefinitions.select(resumed, unseen, false))
        assertNull(GuideDefinitions.select(GuideProgress(GuideStatus.COMPLETED),
            GuideProgress(GuideStatus.SKIPPED), false))
    }

    @Test fun panelPlacementUsesMeasuredHeightAndStaysWithinViewport() {
        assertEquals(116f, guidePanelTop(800f, 200f, 20f, 100f, 16f), 0f)
        assertEquals(384f, guidePanelTop(800f, 200f, 600f, 750f, 16f), 0f)
        assertEquals(300f, guidePanelTop(800f, 200f, null, null, 16f), 0f)
        assertEquals(0f, guidePanelTop(200f, 300f, 50f, 150f, 16f), 0f)
    }
}

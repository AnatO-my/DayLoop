package com.dayloop.app.settings

import org.junit.Assert.*
import org.junit.Test

class GuideProgressTest {
    @Test fun startAdvanceCompleteAndReplay() {
        val unseen = GuideProgress()
        assertTrue(unseen.allowsAutomaticOffer)
        val started = unseen.apply(GuideAction.Start("navigation"))
        assertFalse(started.allowsAutomaticOffer)
        val advanced = started.apply(GuideAction.Advance("create_task"))
        assertEquals("create_task", advanced.stepId)
        val completed = advanced.apply(GuideAction.Complete)
        assertEquals(GuideStatus.COMPLETED, completed.status)
        assertNull(completed.stepId)
        assertEquals(3L, completed.revision)
        assertFalse(completed.allowsAutomaticOffer)
        val replay = completed.apply(GuideAction.Replay("navigation"))
        assertEquals(started.copy(revision = 4), replay)
    }

    @Test fun skipSuppressesAutomaticOfferButAllowsReplay() {
        val skipped = GuideProgress().apply(GuideAction.Skip)
        assertFalse(skipped.allowsAutomaticOffer)
        assertEquals(GuideStatus.IN_PROGRESS, skipped.apply(GuideAction.Replay("navigation")).status)
        assertThrows(IllegalStateException::class.java) { skipped.apply(GuideAction.Start("navigation")) }
        assertThrows(IllegalStateException::class.java) { skipped.apply(GuideAction.Complete) }
    }

    @Test fun malformedIdentifiersAndTransitionsAreRejected() {
        assertThrows(IllegalArgumentException::class.java) { GuideKey("today:v2", 1) }
        assertThrows(IllegalArgumentException::class.java) { GuideKey("today", 0) }
        assertThrows(IllegalArgumentException::class.java) { GuideProgress(GuideStatus.IN_PROGRESS) }
        assertThrows(IllegalArgumentException::class.java) { GuideProgress().apply(GuideAction.Start("")) }
        assertThrows(IllegalStateException::class.java) { GuideProgress().apply(GuideAction.Complete) }
    }
}

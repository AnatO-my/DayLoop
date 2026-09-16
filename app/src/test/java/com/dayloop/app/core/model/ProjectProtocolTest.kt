package com.dayloop.app.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ProjectProtocolTest {

    @Test
    fun `SimplePhaseCycle calculates status correctly for 3-3 cycle`() {
        val anchor = LocalDate.of(2026, 9, 1) // Tue
        val protocol = SimplePhaseCycle(
            anchorDate = anchor,
            activeDays = 3,
            restDays = 3,
            taskTemplate = TaskTemplate("Study")
        )

        // Before start
        assertEquals(ProtocolDayStatus.BEFORE_START, protocol.getStatusAt(anchor.minusDays(1)))

        // First Active phase
        assertEquals(ProtocolDayStatus.ACTIVE(1), protocol.getStatusAt(anchor)) // Sep 1
        assertEquals(ProtocolDayStatus.ACTIVE(2), protocol.getStatusAt(anchor.plusDays(1))) // Sep 2
        assertEquals(ProtocolDayStatus.ACTIVE(3), protocol.getStatusAt(anchor.plusDays(2))) // Sep 3

        // First Rest phase
        assertEquals(ProtocolDayStatus.REST(1), protocol.getStatusAt(anchor.plusDays(3))) // Sep 4
        assertEquals(ProtocolDayStatus.REST(2), protocol.getStatusAt(anchor.plusDays(4))) // Sep 5
        assertEquals(ProtocolDayStatus.REST(3), protocol.getStatusAt(anchor.plusDays(5))) // Sep 6

        // Second cycle
        assertEquals(ProtocolDayStatus.ACTIVE(1), protocol.getStatusAt(anchor.plusDays(6))) // Sep 7
    }

    @Test
    fun `SimplePhaseCycle rejects invalid durations`() {
        val anchor = LocalDate.of(2026, 9, 1)
        
        runCatching {
            SimplePhaseCycle(anchor, activeDays = 0, restDays = 1, TaskTemplate("T"))
        }.also { assertTrue(it.isFailure) }

        runCatching {
            SimplePhaseCycle(anchor, activeDays = 1, restDays = -1, TaskTemplate("T"))
        }.also { assertTrue(it.isFailure) }
    }
}

package com.dayloop.app.settings

private val guideIdentifier = Regex("[a-z][a-z0-9_]{0,63}")

data class GuideKey(val id: String, val version: Int) {
    init {
        require(guideIdentifier.matches(id))
        require(version > 0)
    }
}

enum class GuideStatus { UNSEEN, IN_PROGRESS, SKIPPED, COMPLETED }

data class GuideProgress(
    val status: GuideStatus = GuideStatus.UNSEEN,
    val stepId: String? = null,
    val revision: Long = 0,
) {
    init {
        require(revision >= 0)
        require((status == GuideStatus.IN_PROGRESS) == (stepId != null))
        require(stepId == null || guideIdentifier.matches(stepId))
    }

    // Eligibility also requires a visible target and a non-interrupted host route.
    val allowsAutomaticOffer: Boolean get() = status == GuideStatus.UNSEEN

    fun apply(action: GuideAction): GuideProgress {
        check(revision < Long.MAX_VALUE) { "Guide revision exhausted" }
        val nextRevision = revision + 1
        return when (action) {
            is GuideAction.Start -> {
                check(status == GuideStatus.UNSEEN)
                GuideProgress(GuideStatus.IN_PROGRESS, action.stepId, nextRevision)
            }
            is GuideAction.Advance -> {
                check(status == GuideStatus.IN_PROGRESS)
                GuideProgress(GuideStatus.IN_PROGRESS, action.stepId, nextRevision)
            }
            GuideAction.Complete -> {
                check(status == GuideStatus.IN_PROGRESS)
                GuideProgress(GuideStatus.COMPLETED, revision = nextRevision)
            }
            GuideAction.Skip -> {
                check(status == GuideStatus.UNSEEN || status == GuideStatus.IN_PROGRESS)
                GuideProgress(GuideStatus.SKIPPED, revision = nextRevision)
            }
            is GuideAction.Replay -> GuideProgress(GuideStatus.IN_PROGRESS, action.firstStepId, nextRevision)
        }
    }
}

sealed interface GuideAction {
    data class Start(val stepId: String) : GuideAction
    data class Advance(val stepId: String) : GuideAction
    data object Complete : GuideAction
    data object Skip : GuideAction
    data class Replay(val firstStepId: String) : GuideAction
}

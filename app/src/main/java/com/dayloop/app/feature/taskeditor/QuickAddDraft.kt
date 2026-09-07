package com.dayloop.app.feature.taskeditor

import com.dayloop.app.core.model.Task
import com.dayloop.app.core.model.TaskId

private const val BlankTitleError = "Enter a task title"

data class QuickAddDraft(
    val title: String = "",
    val errorMessage: String? = null,
) {
    val normalizedTitle: String = title.trim()
    val canSubmit: Boolean = normalizedTitle.isNotEmpty()

    fun onTitleChanged(title: String): QuickAddDraft = copy(
        title = title,
        errorMessage = null,
    )

    fun submit(createTaskId: () -> TaskId = { TaskId.generate() }): QuickAddSubmitResult = if (canSubmit) {
        QuickAddSubmitResult.Submitted(
            task = Task(
                id = createTaskId(),
                title = normalizedTitle,
            ),
            nextDraft = QuickAddDraft(),
        )
    } else {
        QuickAddSubmitResult.Invalid(
            draft = copy(errorMessage = BlankTitleError),
        )
    }
}

sealed interface QuickAddSubmitResult {
    data class Submitted(
        val task: Task,
        val nextDraft: QuickAddDraft,
    ) : QuickAddSubmitResult

    data class Invalid(
        val draft: QuickAddDraft,
    ) : QuickAddSubmitResult
}

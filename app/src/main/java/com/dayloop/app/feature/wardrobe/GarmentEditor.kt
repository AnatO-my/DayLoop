package com.dayloop.app.feature.wardrobe

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.dayloop.app.core.model.ClothingCategory
import com.dayloop.app.core.model.ClothingFit
import com.dayloop.app.core.model.ClothingMaterial
import com.dayloop.app.core.model.ClothingPattern
import com.dayloop.app.core.model.GarmentType
import com.dayloop.app.core.model.HexColor
import com.dayloop.app.core.model.Occasion
import com.dayloop.app.core.model.Warmth
import com.dayloop.app.core.outfit.recognition.ColorFamily
import com.dayloop.app.core.outfit.recognition.ConfidenceBand
import com.dayloop.app.core.outfit.recognition.GarmentDraft
import com.dayloop.app.core.outfit.recognition.GarmentPrediction
import com.dayloop.app.core.outfit.recognition.Scored
import com.dayloop.app.ui.theme.dayLoopTokens
import kotlin.math.roundToInt

internal fun ClothingCategory.displayLabel(): String = when (this) {
    ClothingCategory.TOP -> "Tops"
    ClothingCategory.BOTTOM -> "Bottoms"
    ClothingCategory.FOOTWEAR -> "Shoes"
    ClothingCategory.HEADWEAR -> "Headwear"
    ClothingCategory.OUTERWEAR -> "Outerwear"
    ClothingCategory.ACCESSORY -> "Accessories"
}

/** Says plainly how sure the recogniser was, including "it could not tell". */
internal fun <T> confidenceHint(scored: Scored<T>?, label: (T) -> String): String = when {
    scored == null -> "Couldn't tell from the photo. Please choose."
    scored.band == ConfidenceBand.CONFIDENT ->
        "From the photo: ${label(scored.value)} (${(scored.confidence * 100).roundToInt()}% sure). Change it if it's wrong."
    else -> "From the photo: possibly ${label(scored.value)} (${(scored.confidence * 100).roundToInt()}% sure). Please check."
}

/** Opaque Compose colour for "#RRGGBB"; mid-grey for anything else. */
internal fun wardrobeColor(hex: String?): Color = Color(0xFF000000L or (HexColor.parse(hex) ?: 0x808080).toLong())

/**
 * Editable details for one clothing item. Used both to review a recognition draft
 * ([prediction] supplies the "how sure" hints) and to edit an existing item.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun GarmentDraftEditor(
    draft: GarmentDraft,
    onChange: (GarmentDraft) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    prediction: GarmentPrediction? = null,
) {
    val colors = MaterialTheme.dayLoopTokens.colors
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(
            value = draft.name,
            onValueChange = { onChange(draft.copy(name = it.take(80))) },
            singleLine = true,
            enabled = enabled,
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth(),
        )

        Section("Category", prediction?.let { p -> confidenceHint(p.category) { it.displayLabel() } }) {
            ChipChoice(ClothingCategory.entries, draft.category, { it.displayLabel() }, enabled) { chosen ->
                // A category is required, so tapping the selected chip does not clear it.
                if (chosen != null) onChange(draft.withCategory(chosen))
            }
        }

        val types = GarmentType.entries.filter { draft.category == null || it.category == draft.category }
        Section("Type (optional)", prediction?.let { p -> confidenceHint(p.type) { it.label } }) {
            ChipChoice(types, draft.type, { it.label }, enabled) { onChange(draft.withType(it)) }
        }

        val selectedFamily = ColorFamily.ofHex(draft.colorHex)
        val photoColor = prediction?.primaryColor
        Section(
            "Main colour (optional)",
            if (prediction == null) null
            else confidenceHint(photoColor) { it.family.label } + " Lighting can change how colours look.",
        ) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                ColorFamily.entries.forEach { family ->
                    val selected = family == selectedFamily
                    // Keep the exact shade seen in the photo when its family is chosen.
                    val shade = photoColor?.value?.takeIf { it.family == family }?.hex ?: family.swatchHex
                    Box(
                        Modifier.size(48.dp)
                            .selectable(selected, enabled = enabled, role = Role.RadioButton) {
                                onChange(draft.copy(colorHex = if (selected) null else shade))
                            }
                            .semantics { contentDescription = family.label },
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            Modifier.size(if (selected) 38.dp else 30.dp).clip(CircleShape)
                                .background(wardrobeColor(if (selected) draft.colorHex ?: shade else shade))
                                .border(if (selected) 3.dp else 1.dp, if (selected) colors.accentText else colors.secondaryText, CircleShape)
                        )
                    }
                }
            }
            Text(
                selectedFamily?.let { "Selected: ${it.label}" } ?: "No colour selected",
                style = MaterialTheme.typography.bodySmall, color = colors.secondaryText,
            )
            draft.secondaryColorHex?.let { second ->
                TextButton(enabled = enabled, onClick = { onChange(draft.copy(secondaryColorHex = null)) }) {
                    Text("Second colour: ${ColorFamily.ofHex(second)?.label ?: second}. Tap to remove", color = colors.accentText)
                }
            }
        }

        Section("Pattern (optional)", prediction?.let { p -> confidenceHint(p.pattern) { it.label } }) {
            ChipChoice(ClothingPattern.entries, draft.pattern, { it.label }, enabled) { onChange(draft.copy(pattern = it)) }
        }

        Section(
            "Material (optional)",
            prediction?.materialHint?.let {
                "The photo hints at ${it.value.label.lowercase()}, but material can't be confirmed from a picture. Choose it only if you know."
            } ?: if (prediction != null) "Material can't be told from a photo. Choose it if you know." else null,
        ) {
            ChipChoice(ClothingMaterial.entries, draft.material, { it.label }, enabled) { onChange(draft.copy(material = it)) }
        }

        Section("Warmth (optional)", "Leave empty to use what is typical for the type.") {
            ChipChoice(Warmth.entries, draft.warmth, { it.label }, enabled) { onChange(draft.copy(warmth = it)) }
        }

        Section("Occasions (optional)", "Pick all that apply, or leave empty to use what is typical for the type.") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Occasion.entries.forEach { occasion ->
                    val selected = occasion in draft.occasions
                    FilterChip(
                        selected = selected,
                        enabled = enabled,
                        onClick = { onChange(draft.copy(occasions = if (selected) draft.occasions - occasion else draft.occasions + occasion)) },
                        label = { Text(occasion.label) },
                    )
                }
            }
        }

        Section("Fit (optional)", null) {
            ChipChoice(ClothingFit.entries, draft.fit, { it.label }, enabled) { onChange(draft.copy(fit = it)) }
        }
    }
}

@Composable
internal fun Section(title: String, hint: String?, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.semantics { heading() })
        hint?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.dayLoopTokens.colors.secondaryText)
        }
        content()
    }
}

/** Single-choice chips. Tapping the selected chip reports null ("not set"). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun <T> ChipChoice(
    options: List<T>,
    selected: T?,
    label: (T) -> String,
    enabled: Boolean,
    onSelect: (T?) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                enabled = enabled,
                onClick = { onSelect(if (option == selected) null else option) },
                label = { Text(label(option)) },
            )
        }
    }
}

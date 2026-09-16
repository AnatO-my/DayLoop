package com.dayloop.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.currentState
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.action.clickable
import androidx.glance.action.actionStartActivity
import com.dayloop.app.MainActivity
import com.dayloop.app.R

class GoalWidget : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                GoalWidgetContent()
            }
        }
    }

    @Composable
    private fun GoalWidgetContent() {
        val prefs = currentState<Preferences>()
        val title = prefs[GoalTitleKey] ?: "No active goal"
        val progress = prefs[GoalProgressKey] ?: "0%"
        val progressValue = (prefs[GoalProgressValueKey] ?: "0").toInt().coerceIn(0, 100)

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(R.color.dayloop_dark))
                .padding(16.dp)
                .clickable(actionStartActivity<MainActivity>()),
            verticalAlignment = Alignment.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(modifier = GlanceModifier.defaultWeight(), verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_nav_goals),
                        contentDescription = null,
                        modifier = GlanceModifier.width(16.dp).height(16.dp)
                    )
                    Spacer(GlanceModifier.width(4.dp))
                    Text(
                        text = "MY GOAL",
                        style = TextStyle(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorProvider(R.color.dayloop_lime)
                        )
                    )
                }
            }
            
            Spacer(GlanceModifier.defaultWeight())
            
            Text(
                text = title,
                style = TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorProvider(R.color.white)
                ),
                maxLines = 2
            )
            
            Spacer(GlanceModifier.height(12.dp))
            
            // Simple progress bar using weight
            Row(modifier = GlanceModifier.fillMaxWidth().height(6.dp).background(ColorProvider(R.color.white)).padding(0.dp)) {
                if (progressValue > 0) {
                    Box(modifier = GlanceModifier.defaultWeight().fillMaxHeight().background(ColorProvider(R.color.dayloop_lime))) {}
                }
                // This is a bit flawed but limited by Glance layout
            }
            
            Spacer(GlanceModifier.height(4.dp))
            
            Text(
                text = progress,
                style = TextStyle(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = ColorProvider(R.color.dayloop_lime)
                )
            )
            
            Spacer(GlanceModifier.defaultWeight())
            
            Text(
                text = "Keep going! ✨",
                style = TextStyle(
                    fontSize = 11.sp,
                    color = ColorProvider(R.color.white)
                )
            )
        }
    }

    companion object {
        val GoalTitleKey = stringPreferencesKey("goal_title")
        val GoalProgressKey = stringPreferencesKey("goal_progress")
        val GoalProgressValueKey = stringPreferencesKey("goal_progress_value")
    }
}

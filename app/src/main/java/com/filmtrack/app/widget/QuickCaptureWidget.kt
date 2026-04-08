package com.filmtrack.app.widget

import android.content.Context
import android.content.Intent
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.filmtrack.app.MainActivity
import com.filmtrack.app.R

class QuickCaptureWidget : GlanceAppWidget() {

    companion object {
        val SMALL = DpSize(57.dp, 57.dp)
        val NORMAL = DpSize(110.dp, 57.dp)
    }

    override val sizeMode = SizeMode.Responsive(setOf(SMALL, NORMAL))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                val size = LocalSize.current
                val isSmall = size.width < 100.dp

                if (isSmall) {
                    // 1x1: just camera icon + "FilmTrack" label
                    Column(
                        modifier = GlanceModifier
                            .fillMaxSize()
                            .background(GlanceTheme.colors.widgetBackground)
                            .cornerRadius(16.dp)
                            .clickable(actionRunCallback<QuickCaptureAction>())
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            provider = ImageProvider(R.drawable.ic_camera_widget),
                            contentDescription = "Capture",
                            modifier = GlanceModifier.size(28.dp)
                        )
                        Spacer(modifier = GlanceModifier.size(4.dp))
                        Text(
                            text = "FilmTrack",
                            style = TextStyle(
                                fontWeight = FontWeight.Medium,
                                fontSize = 9.sp,
                                color = GlanceTheme.colors.onSurface
                            )
                        )
                    }
                } else {
                    // Normal size: icon + "FilmTrack" / "Quick Capture"
                    Column(
                        modifier = GlanceModifier
                            .fillMaxSize()
                            .background(GlanceTheme.colors.widgetBackground)
                            .cornerRadius(16.dp)
                            .clickable(actionRunCallback<QuickCaptureAction>())
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Image(
                                provider = ImageProvider(R.drawable.ic_camera_widget),
                                contentDescription = "Capture",
                                modifier = GlanceModifier.size(32.dp)
                            )
                            Spacer(modifier = GlanceModifier.width(12.dp))
                            Column {
                                Text(
                                    text = "FilmTrack",
                                    style = TextStyle(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = GlanceTheme.colors.onSurface
                                    )
                                )
                                Text(
                                    text = "Quick Capture",
                                    style = TextStyle(
                                        fontSize = 12.sp,
                                        color = GlanceTheme.colors.secondary
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

class QuickCaptureAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = "com.filmtrack.app.QUICK_CAPTURE"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        context.startActivity(intent)
    }
}

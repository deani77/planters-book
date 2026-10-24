package com.selendang3.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.cos
import kotlin.math.sin

// Original local vector symbols; the button label supplies the accessible name.
@Composable
fun EstateModuleIcon(route: String, modifier: Modifier, color: Color) {
    if (route == "tools") {
        Box(modifier, contentAlignment = Alignment.Center) {
            Text("ƒ(x)", style = MaterialTheme.typography.titleMedium, color = color)
        }
    } else Canvas(modifier) {
        val u = size.minDimension / 24f
        fun p(x: Float, y: Float) = Offset(x * u, y * u)
        when (route) {
            "information" -> {
                val leaf = Path().apply {
                    moveTo(3*u, 2*u); cubicTo(3*u, 14*u, 4*u, 22*u, 19*u, 21*u)
                    cubicTo(24*u, 6*u, 12*u, 7*u, 3*u, 2*u); close()
                }
                drawPath(leaf, color)
                drawLine(color, p(8f, 12f), p(22f, 23f), 2*u, StrokeCap.Round)
            }
            "production" -> for (row in 0..1) for (col in 0..1) {
                drawRoundRect(color, p(2f + col*12f, 2f + row*12f),
                    Size(8*u, 8*u), CornerRadius(2*u, 2*u))
            }
            "upkeep" -> {
                drawLine(color, p(12f, 10f), p(12f, 22f), 3*u, StrokeCap.Round)
                drawLine(color, p(6f, 22f), p(18f, 22f), 2*u, StrokeCap.Round)
                drawLine(color, p(8f, 11f), p(16f, 11f), 2*u, StrokeCap.Round)
                for (index in 0..6) {
                    val angle = Math.PI + index * Math.PI / 6
                    drawCircle(color, u, Offset((12 + cos(angle)*9).toFloat()*u,
                        (11 + sin(angle)*9).toFloat()*u))
                }
            }
            "reports", "dashboard" -> {
                listOf(0.45f, 0.72f, 1f).forEachIndexed { index, fraction ->
                    drawRoundRect(color, p(2f + index*7f, 20f - 17f*fraction),
                        Size(5*u, 17*u*fraction), CornerRadius(u, u))
                }
                drawLine(color, p(1f, 23f), p(23f, 23f), 1.5f*u, StrokeCap.Round)
            }
            "worker" -> {
                drawCircle(color, 4*u, p(12f, 6f))
                drawRoundRect(color, p(5f, 12f), Size(14*u, 10*u), CornerRadius(4*u, 4*u))
            }
            "pest" -> {
                drawCircle(color, 3*u, p(12f, 5f))
                drawRoundRect(color, p(8f, 8f), Size(8*u, 13*u), CornerRadius(4*u, 4*u))
                for (y in listOf(10f, 15f, 19f)) {
                    drawLine(color, p(8f, y), p(4f, y+2f), 1.5f*u, StrokeCap.Round)
                    drawLine(color, p(16f, y), p(20f, y+2f), 1.5f*u, StrokeCap.Round)
                }
            }
            "cost" -> {
                drawRoundRect(color, p(1f, 5f), Size(22*u, 14*u), CornerRadius(2*u, 2*u),
                    style = Stroke(1.5f*u))
                drawCircle(color, 4*u, p(12f, 12f), style = Stroke(1.5f*u))
            }
            "settings" -> {
                drawCircle(color, 6*u, p(12f, 12f), style = Stroke(4*u))
                for (index in 0..7) {
                    val angle = index * Math.PI / 4
                    drawLine(color, Offset((12+cos(angle)*7).toFloat()*u, (12+sin(angle)*7).toFloat()*u),
                        Offset((12+cos(angle)*10).toFloat()*u, (12+sin(angle)*10).toFloat()*u),
                        3*u, StrokeCap.Round)
                }
            }
            else -> {
                drawRoundRect(color, p(3f, 3f), Size(18*u, 18*u), CornerRadius(3*u, 3*u),
                    style = Stroke(2*u))
                drawLine(color, p(8f, 9f), p(16f, 9f), 2*u)
                drawLine(color, p(8f, 15f), p(16f, 15f), 2*u)
            }
        }
    }
}

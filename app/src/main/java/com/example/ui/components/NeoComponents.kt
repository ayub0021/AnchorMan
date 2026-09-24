package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AnchorBorder
import com.example.ui.theme.AnchorCyan
import com.example.ui.theme.AnchorDark
import com.example.ui.theme.AnchorYellow
import kotlin.math.roundToInt

@Composable
fun NeoCard(
    modifier: Modifier = Modifier,
    shape: CornerBasedShape = RoundedCornerShape(14.dp),
    backgroundColor: Color = Color.White,
    borderColor: Color = AnchorBorder,
    shadowColor: Color = AnchorDark,
    borderWidth: Dp = 2.dp,
    shadowOffset: Dp = 4.dp,
    testTag: String = "neo_card",
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .padding(end = shadowOffset, bottom = shadowOffset)
            .testTag(testTag)
    ) {
        // Hard drop shadow layer
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = shadowOffset, y = shadowOffset)
                .background(shadowColor, shape)
        )
        // Main surface layer
        Box(
            modifier = Modifier
                .background(backgroundColor, shape)
                .border(borderWidth, borderColor, shape)
                .clip(shape),
            content = content
        )
    }
}

@Composable
fun NeoButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = AnchorCyan,
    borderColor: Color = AnchorBorder,
    shadowColor: Color = AnchorDark,
    shape: CornerBasedShape = RoundedCornerShape(12.dp),
    borderWidth: Dp = 2.dp,
    shadowOffset: Dp = 4.dp,
    testTag: String = "neo_button",
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val currentOffset by animateFloatAsState(
        targetValue = if (isPressed) shadowOffset.value else 0f,
        animationSpec = spring(stiffness = 500f),
        label = "btn_press_anim"
    )

    Box(
        modifier = modifier
            .padding(end = shadowOffset, bottom = shadowOffset)
            .testTag(testTag)
    ) {
        // Shadow base
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = shadowOffset, y = shadowOffset)
                .background(shadowColor, shape)
        )
        // Tactile button top
        Box(
            modifier = Modifier
                .offset { IntOffset(currentOffset.roundToInt(), currentOffset.roundToInt()) }
                .background(backgroundColor, shape)
                .border(borderWidth, borderColor, shape)
                .clip(shape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center,
            content = content
        )
    }
}

@Composable
fun CategoryPill(
    label: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = AnchorCyan,
    textColor: Color = AnchorDark,
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val shape = RoundedCornerShape(8.dp)
    val baseModifier = if (onClick != null) {
        modifier
            .clickable { onClick() }
    } else {
        modifier
    }

    Box(
        modifier = baseModifier
            .padding(end = 2.dp, bottom = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 2.dp, y = 2.dp)
                .background(AnchorDark, shape)
        )
        Box(
            modifier = Modifier
                .background(if (isSelected) AnchorYellow else backgroundColor, shape)
                .border(1.5.dp, AnchorDark, shape)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = label,
                color = textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

/**
 * Geometric modern Anchor symbol - NOT an emoji.
 * Inspired by defensive midfielder / interception and subtle naval telemetry.
 */
@Composable
fun AnchorLogoSymbol(
    modifier: Modifier = Modifier.size(28.dp),
    accentColor: Color = AnchorCyan,
    strokeColor: Color = AnchorDark
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = 2.5.dp.toPx()

        // Top eye ring
        val ringCenter = Offset(w * 0.5f, h * 0.22f)
        val ringRadius = w * 0.14f

        drawCircle(
            color = accentColor,
            radius = ringRadius,
            center = ringCenter
        )
        drawCircle(
            color = strokeColor,
            radius = ringRadius,
            center = ringCenter,
            style = Stroke(width = stroke)
        )
        // Center core dot
        drawCircle(
            color = strokeColor,
            radius = ringRadius * 0.4f,
            center = ringCenter
        )

        // Vertical core shaft
        drawLine(
            color = strokeColor,
            start = Offset(w * 0.5f, ringCenter.y + ringRadius),
            end = Offset(w * 0.5f, h * 0.85f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )

        // Horizontal radar crossbar
        drawLine(
            color = strokeColor,
            start = Offset(w * 0.30f, h * 0.45f),
            end = Offset(w * 0.70f, h * 0.45f),
            strokeWidth = stroke,
            cap = StrokeCap.Square
        )

        // Cyan crossbar accent nodes
        drawCircle(
            color = accentColor,
            radius = w * 0.05f,
            center = Offset(w * 0.30f, h * 0.45f)
        )
        drawCircle(
            color = strokeColor,
            radius = w * 0.05f,
            center = Offset(w * 0.30f, h * 0.45f),
            style = Stroke(width = 1.5.dp.toPx())
        )
        drawCircle(
            color = accentColor,
            radius = w * 0.05f,
            center = Offset(w * 0.70f, h * 0.45f)
        )
        drawCircle(
            color = strokeColor,
            radius = w * 0.05f,
            center = Offset(w * 0.70f, h * 0.45f),
            style = Stroke(width = 1.5.dp.toPx())
        )

        // Sweeping interception arc (flukes)
        val arcPath = Path().apply {
            moveTo(w * 0.16f, h * 0.65f)
            cubicTo(
                w * 0.25f, h * 0.90f,
                w * 0.75f, h * 0.90f,
                w * 0.84f, h * 0.65f
            )
        }
        drawPath(
            path = arcPath,
            color = strokeColor,
            style = Stroke(width = stroke * 1.1f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Interceptor arrowheads on fluke tips
        drawCircle(
            color = accentColor,
            radius = w * 0.06f,
            center = Offset(w * 0.16f, h * 0.65f)
        )
        drawCircle(
            color = strokeColor,
            radius = w * 0.06f,
            center = Offset(w * 0.16f, h * 0.65f),
            style = Stroke(width = 1.5.dp.toPx())
        )
        drawCircle(
            color = accentColor,
            radius = w * 0.06f,
            center = Offset(w * 0.84f, h * 0.65f)
        )
        drawCircle(
            color = strokeColor,
            radius = w * 0.06f,
            center = Offset(w * 0.84f, h * 0.65f),
            style = Stroke(width = 1.5.dp.toPx())
        )
    }
}

/**
 * Comic-style "CAUGHT!" burst badge animation
 */
@Composable
fun ComicBurstCaughtBadge(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit
) {
    val scale = remember { Animatable(0f) }
    val rotation = remember { Animatable(-15f) }

    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1.15f,
            animationSpec = spring(dampingRatio = 0.5f, stiffness = 600f)
        )
        scale.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(100)
        )
        kotlinx.coroutines.delay(1100)
        scale.animateTo(
            targetValue = 0f,
            animationSpec = tween(200, easing = FastOutSlowInEasing)
        )
        onDismiss()
    }

    Box(
        modifier = modifier
            .scale(scale.value)
            .rotate(rotation.value),
        contentAlignment = Alignment.Center
    ) {
        // Jagged comic starburst back
        Canvas(modifier = Modifier.size(130.dp, 64.dp)) {
            val w = size.width
            val h = size.height

            // Hard black shadow burst
            val shadowPath = createBurstPath(w, h, offsetX = 4.dp.toPx(), offsetY = 4.dp.toPx())
            drawPath(path = shadowPath, color = AnchorDark)

            // Yellow/Cyan burst
            val burstPath = createBurstPath(w, h)
            drawPath(path = burstPath, color = AnchorYellow)
            drawPath(path = burstPath, color = AnchorDark, style = Stroke(width = 2.5.dp.toPx()))
        }

        Text(
            text = "CAUGHT.",
            color = AnchorDark,
            fontWeight = FontWeight.Black,
            fontSize = 20.sp,
            letterSpacing = 1.sp
        )
    }
}

private fun createBurstPath(width: Float, height: Float, offsetX: Float = 0f, offsetY: Float = 0f): Path {
    val path = Path()
    val points = listOf(
        Offset(0.05f, 0.5f),
        Offset(0.12f, 0.22f),
        Offset(0.28f, 0.28f),
        Offset(0.48f, 0.10f),
        Offset(0.68f, 0.26f),
        Offset(0.86f, 0.16f),
        Offset(0.95f, 0.46f),
        Offset(0.88f, 0.74f),
        Offset(0.70f, 0.70f),
        Offset(0.50f, 0.90f),
        Offset(0.32f, 0.72f),
        Offset(0.14f, 0.80f)
    )

    points.forEachIndexed { index, p ->
        val x = p.x * width + offsetX
        val y = p.y * height + offsetY
        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

/**
 * Subtle halftone texture drawn behind sections to give subtle manga/comic depth.
 */
@Composable
fun ComicHalftoneOverlay(
    modifier: Modifier = Modifier,
    dotColor: Color = Color(0x0C181818),
    spacing: Dp = 12.dp,
    dotRadius: Dp = 1.5.dp
) {
    Canvas(modifier = modifier) {
        val sp = spacing.toPx()
        val r = dotRadius.toPx()
        var y = sp / 2
        while (y < size.height) {
            var x = sp / 2
            while (x < size.width) {
                drawCircle(
                    color = dotColor,
                    radius = r,
                    center = Offset(x, y)
                )
                x += sp
            }
            y += sp
        }
    }
}

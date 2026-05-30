package com.example.doshka.ui.screens.splash

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.doshka.R
import com.example.doshka.ui.theme.DoshkaTheme

@Composable
fun SplashScreen(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF4B79E4),
                        Color(0xFF1B3061)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            KanbanIllustration()
            
            Spacer(modifier = Modifier.height(64.dp))
            
            LogoText()
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = stringResource(id = R.string.splash_subtitle),
                color = Color(0xFFA0B3D6),
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun KanbanIllustration() {
    Box(
        modifier = Modifier
            .size(320.dp, 240.dp)
            .shadow(32.dp, RoundedCornerShape(32.dp))
            .clip(RoundedCornerShape(32.dp))
            .background(Color(0xFF152243))
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // To Do Column
            KanbanColumn(
                title = stringResource(id = R.string.column_todo_en),
                headerColor = Color(0xFF3A7CF3),
                cards = listOf(
                    CardData(dotColor = Color(0xFF3A7CF3)),
                    CardData(dotColor = Color(0xFF3A7CF3))
                ),
                modifier = Modifier.weight(1f)
            )

            // In Progress Column
            KanbanColumn(
                title = stringResource(id = R.string.column_in_progress_en),
                headerColor = Color(0xFFFDB813),
                cards = listOf(
                    CardData(dotColor = Color(0xFFFDB813)),
                    CardData(dotColor = Color(0xFFFDB813), isMoving = true)
                ),
                modifier = Modifier.weight(1f)
            )

            // Done Column
            KanbanColumn(
                title = stringResource(id = R.string.column_done_en),
                headerColor = Color(0xFF4CAF50),
                cards = listOf(
                    CardData(dotColor = Color(0xFF4CAF50)),
                    CardData(dotColor = Color(0xFF4CAF50))
                ),
                modifier = Modifier.weight(1f)
            )
        }

        // Dashed Arrow
        DashedArrow(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(x = 45.dp, y = (-35).dp)
                .size(100.dp, 40.dp)
        )
    }
}

@Composable
private fun KanbanColumn(
    title: String,
    headerColor: Color,
    cards: List<CardData>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(headerColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        cards.forEach { card ->
            KanbanCard(card)
        }
    }
}

private data class CardData(
    val dotColor: Color,
    val isMoving: Boolean = false
)

@Composable
private fun KanbanCard(card: CardData) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .then(
                if (card.isMoving) {
                    Modifier
                        .graphicsLayer {
                            rotationZ = -8f
                            translationY = 25f
                            translationX = 15f
                        }
                        .shadow(12.dp, RoundedCornerShape(10.dp))
                } else {
                    Modifier.shadow(2.dp, RoundedCornerShape(10.dp))
                }
            )
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White)
            .padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(card.dotColor)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFFE0E0E0))
                )
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFFE0E0E0))
                )
            }
        }
    }
}

@Composable
private fun DashedArrow(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val path = Path().apply {
            moveTo(0f, 0f)
            quadraticTo(
                size.width * 0.4f, size.height * 0.8f,
                size.width * 0.8f, size.height * 0.2f
            )
        }
        
        drawPath(
            path = path,
            color = Color.White.copy(alpha = 0.5f),
            style = Stroke(
                width = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )
        )
        
        // Arrow head
        val arrowSize = 8.dp.toPx()
        val arrowPath = Path().apply {
            moveTo(size.width * 0.8f, size.height * 0.2f)
            lineTo(size.width * 0.8f - arrowSize, size.height * 0.2f - arrowSize * 0.2f)
            moveTo(size.width * 0.8f, size.height * 0.2f)
            lineTo(size.width * 0.8f - arrowSize * 0.2f, size.height * 0.2f + arrowSize)
        }
        
        // Drawing a small triangle for the arrow head
        val headPath = Path().apply {
            moveTo(size.width * 0.82f, size.height * 0.18f)
            lineTo(size.width * 0.72f, size.height * 0.15f)
            lineTo(size.width * 0.78f, size.height * 0.28f)
            close()
        }
        drawPath(
            path = headPath,
            color = Color.White.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun LogoText() {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            text = "Dosh",
            color = Color.White,
            fontSize = 86.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-2).sp
        )
        Text(
            text = "Ka",
            color = Color(0xFF3A7CF3),
            fontSize = 86.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-2).sp
        )
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 800)
@Composable
fun SplashScreenPreview() {
    DoshkaTheme {
        SplashScreen()
    }
}

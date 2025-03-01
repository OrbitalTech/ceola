package com.orbital.cee.view.home.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orbital.cee.R


@Composable
fun ReliabilityProgressRatio(likeCount: Int, dislikeCount: Int) {
    val total = likeCount + dislikeCount
    val likePercentage = if (total > 0) likeCount.toFloat() / total.toFloat() else 0f

    val capsuleColor = when {
        likePercentage < 0.25 -> Color(0xFFEA4E34)
        likePercentage < 0.5 -> Color(0xFFF27D28)
        likePercentage < 0.75 -> Color(0xFF36B5FF)
        else -> Color(0xFF495CE8)
    }

    val reliabilityText = when {
        likePercentage < 0.25 -> stringResource(id = R.string.lbl_low)
        likePercentage < 0.5 -> stringResource(id = R.string.lbl_medium)
        likePercentage < 0.75 -> stringResource(id = R.string.lbl_high)
        else -> stringResource(id = R.string.lbl_very_High)
    }

    Column(verticalArrangement = Arrangement.Center) {
        Row {
            Text("${stringResource(id = R.string.lbl_reliability)}: ",fontSize = 16.sp, fontWeight = FontWeight.W500,color = Color(0xFF171729))
            Text(reliabilityText, color = capsuleColor,fontSize = 16.sp, fontWeight = FontWeight.W500)
        }
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .height(14.dp)
                        .clip(CircleShape)
                        .background(capsuleColor.copy(alpha = 0.5f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(likePercentage)
                            .height(14.dp)
                            .clip(CircleShape)
                            .background(capsuleColor)
                    )
                }
                Spacer(modifier = Modifier.width(5.dp))
                Row(horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically,modifier = Modifier
                    .wrapContentWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(painterResource(id = R.drawable.ic_like), contentDescription = null, tint = Color.Unspecified)
                        Text("($likeCount)", color = Color(0xFF57D654),lineHeight = 16.sp,fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.width(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(painterResource(id = R.drawable.ic_dislike), contentDescription = null,tint = Color.Unspecified)
                        Text("($dislikeCount)", color = Color(0xFFEA4E34),lineHeight = 16.sp,fontSize = 14.sp)
                    }
                }
            }
        }

    }
}
@Composable
fun progressRatioCeeKer(currentPoints: Int, nextLevelPoints: Int) {
    val total = currentPoints + nextLevelPoints
    val likePercentage = if (total > 0) currentPoints.toFloat() / total.toFloat() else 0f

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .height(14.dp)
                .clip(CircleShape)
                .background(Color(0x3457D654))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(likePercentage)
                    .height(14.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF57D654))
            )
        }
    }
}
@Preview
@Composable
fun prev(){
    progressRatioCeeKer(20, 10)
}
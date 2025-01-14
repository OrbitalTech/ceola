package com.orbital.cee.view.home.BottomSheets

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orbital.cee.R
import com.orbital.cee.core.MyLocationService
import com.orbital.cee.model.AlarmLessReports
import com.orbital.cee.model.NewReport
import com.orbital.cee.model.SingleCustomReport
import com.orbital.cee.model.UserTiers
import com.orbital.cee.ui.theme.blurple
import com.orbital.cee.ui.theme.red
import com.orbital.cee.utils.MetricsUtils.Companion.getReportUiByReportType
import com.orbital.cee.utils.Utils.getNavigatingBarHeight
import com.orbital.cee.utils.Utils.pxToDp
import com.orbital.cee.view.home.HomeViewModel
import com.orbital.cee.view.home.appMenu.componenets.radio
import com.orbital.cee.view.home.UserDao
import com.orbital.cee.view.home.components.DisLikeButton
import com.orbital.cee.view.home.components.LikeButton
import com.orbital.cee.view.home.components.Loading
import com.orbital.cee.view.home.components.ReliabilityProgressRatio
import com.orbital.cee.view.home.components.ReportIconWithSpeedLimit
import com.orbital.cee.view.home.data_Store
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.math.RoundingMode
import java.text.DecimalFormat
import java.util.*
import kotlin.collections.ArrayList

@SuppressLint("CoroutineCreationDuringComposition")
@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun NewReportViewDetail(vModel : HomeViewModel,
                        reportId:MutableState<String>,
                        onReportDelete:(id:String)->Unit,
                        onEditSpeedLimit: (reportId : String,speed:Int?)->Unit,
                        onClickSendNotification:(userId :String)->Unit
){

    val isLoading = remember { mutableStateOf(true) }
    val bottomNavigationBarHeight = remember { mutableStateOf(0) }
    val isError = remember { mutableStateOf(false) }
    val context = LocalContext.current
    val isLike = remember { mutableStateOf<Boolean?>(null) }
    val isShowFeedBackPanel = remember { mutableStateOf(false) }
    val isShowFeedBack = remember { mutableStateOf(false) }
    val isChangeDetacted = remember { mutableStateOf(false) }
    val isMuted = remember { mutableStateOf(false) }
    val dislikeReason = remember { mutableStateOf<Int?>(null) }
    val pReportId = remember {mutableStateOf("")}
    val report = remember { mutableStateOf(SingleCustomReport(isSuccess = false))}
    val localReport = remember { mutableStateOf<NewReport?>(null)}
    val reportOwnerInfo = remember {mutableStateOf(UserDao())}
    val mutedReports =context.data_Store.data.collectAsState(initial = AlarmLessReports()).value
    if (mutedReports.mutedReports.isNotEmpty()){
        MyLocationService.LSR.allMutedReports.addAll(mutedReports.mutedReports)
    }else{
        MyLocationService.LSR.allMutedReports.clear()
    }
    LaunchedEffect(Unit){
        bottomNavigationBarHeight.value = pxToDp(getNavigatingBarHeight(context))
    }

    val scroll = rememberScrollState()
    val df = DecimalFormat("#.#")
    df.roundingMode = RoundingMode.UP
    val likePercent = remember {
        mutableStateOf(0.0f)
    }
    val disLikePercent = remember {
        mutableStateOf(0.0f)
    }
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val coroutineScope = rememberCoroutineScope()

    isMuted.value = MyLocationService.LSR.isThisReportMuted(reportId = reportId.value)
    Log.d("NRepo","NEW: "+reportId.value)
    if(pReportId.value != reportId.value){
        isLoading.value = true
        MyLocationService.LSR.getReportDataById(reportId.value)?.let {
            localReport.value = it
        }
        coroutineScope.launch {
            vModel.getSingleReport(reportId = reportId.value).collect{
                isLoading.value = true
                if (it.isSuccess){
                    report.value = it
                    isLike.value = it.isLiked
                    likePercent.value = 0.001f.coerceAtLeast (it.feedbackLikeCount.toFloat()/ (it.feedbackLikeCount.toFloat() + it.feedbackDisLikeCount.toFloat()))
                    disLikePercent.value =0.001f.coerceAtLeast (it.feedbackDisLikeCount.toFloat()/(it.feedbackLikeCount.toFloat()+it.feedbackDisLikeCount.toFloat()))
                    if (it.reportByUID != ""){
                        reportOwnerInfo.value = vModel.getReportOwnerByUid(it.reportByUID)
                        isLoading.value = false
                    }
                    delay(500)
                    report.value.reportLocation?.let {loco->
                        if(vModel.currentUserTier.value != UserTiers.ADMIN){
                            isShowFeedBack.value = loco.distanceTo(vModel.lastLocation.value).div(1000)<= 1.0
                        }else{
                            isShowFeedBack.value = true
                        }
                    }
                    isLoading.value = false
                }
            }
        }
        Log.d("NRepo","PAS: "+pReportId.value)
        isError.value = false
        isShowFeedBackPanel.value = false
        //isLiked.value = null
        isLike.value = null
        dislikeReason.value = null
        pReportId.value = reportId.value

    }
    val reportUI = getReportUiByReportType(reportType =localReport.value?.reportType ?: 1, context = context)
    Box(modifier = Modifier
        .fillMaxWidth()
        .clip(shape = RoundedCornerShape(topStart = 15.dp, topEnd = 15.dp))
        .background(color = Color.White), contentAlignment = Alignment.Center){
        if (false){
            Column(modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp)) {
                Row(
                    Modifier
                        .fillMaxWidth(0.6f)
                        .height(20.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .shimmerEffect()){}
                Spacer(modifier = Modifier.height(25.dp))
                Row(Modifier.fillMaxWidth()) {
                    Box(
                        Modifier
                            .width(60.dp)
                            .height(60.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .shimmerEffect())
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            Modifier
                                .width(150.dp)
                                .height(15.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .shimmerEffect()){}
                        Spacer(modifier = Modifier.height(5.dp))
                        Row(
                            Modifier
                                .width(120.dp)
                                .height(15.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .shimmerEffect()){}

                    }
                }
                Spacer(modifier = Modifier.height(35.dp))

                Row(horizontalArrangement = Arrangement.SpaceBetween) {
                    Box(modifier = Modifier
                        .height(18.dp)
                        .clip(RoundedCornerShape(35.dp))
                        .fillMaxWidth(0.80f)
                        .shimmerEffect())
                    Spacer(modifier = Modifier.width(5.dp))
                    Box(modifier = Modifier
                        .height(18.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .fillMaxWidth(0.50f)
                        .shimmerEffect())
                    Spacer(modifier = Modifier.width(5.dp))
                    Box(modifier = Modifier
                        .height(18.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .fillMaxWidth()
                        .shimmerEffect())
                }
                Spacer(modifier = Modifier.height(15.dp))

                Divider(thickness = 2.dp, color = Color(0XFFF7F7F7))
                Spacer(modifier = Modifier.height(20.dp))
                Column {
                    Row(
                        Modifier
                            .width(150.dp)
                            .height(15.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .shimmerEffect()){}
                    Spacer(modifier = Modifier.height(5.dp))
                    Row(
                        Modifier
                            .width(120.dp)
                            .height(15.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .shimmerEffect()){}

                }
                Spacer(modifier = Modifier.height(15.dp))

                Row(horizontalArrangement = Arrangement.SpaceBetween) {
                    Box(modifier = Modifier
                        .height(45.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .fillMaxWidth(0.5f)
                        .shimmerEffect())
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(modifier = Modifier
                        .height(45.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .fillMaxWidth()
                        .shimmerEffect())
                }

            }
        }else{
            Column(modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp)) {

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    localReport.value?.reportTimeStamp?.let {
                    Text(text = vModel.incidentTime(it, context) , color = Color(0XFF848484), fontWeight = FontWeight.W300)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
                        if(vModel.currentUserTier.value == UserTiers.ADMIN){
                            IconButton(onClick ={
                                onClickSendNotification(report.value.reportByUID)
                            } ) {
                                Icon(modifier = Modifier.size(20.dp),painter = painterResource(id = R.drawable.ic_flash), contentDescription = "", tint = blurple)
                            }
                            IconButton(onClick ={
                                val myUrl = "https://cee-platform-87d21.web.app/report/${reportId.value}"
                                val shareIntent = Intent()
                                shareIntent.action = Intent.ACTION_SEND
                                shareIntent.type="text/plain"
                                shareIntent.putExtra(Intent.EXTRA_TEXT, myUrl)
                                shareIntent.putExtra(Intent.EXTRA_TITLE, "share report")
                                context.startActivity(Intent.createChooser(shareIntent,myUrl))
//                                onClickSendNotification(report.value.reportByUID)
                            } ) {
                                Icon(modifier = Modifier.size(20.dp),painter = painterResource(id = R.drawable.ic_share), contentDescription = "", tint = red)
                            }
                            IconButton(onClick ={
                                vModel.extendReportTime(reportId.value)
                            } ) {
                                Icon(modifier = Modifier.size(20.dp),painter = painterResource(id = R.drawable.refresh_circle), contentDescription = "", tint = Color(0xFF495CE8))
                            }
                            IconButton(onClick ={
                                onEditSpeedLimit(reportId.value,report.value.reportSpeedLimit)
                            } ) {
                                Icon(modifier = Modifier.size(20.dp),painter = painterResource(id = R.drawable.ic_pen), contentDescription = "", tint = Color(0xFF495CE8))
                            }

                        }
                        if (report.value.reportType == 5 || report.value.reportType == 6 || report.value.reportType == 4){
                            if(isMuted.value){
                                IconButton(onClick ={
                                    scope.launch {
                                        unMuteReport(reportId.value, context = context)
                                    }
                                    isChangeDetacted.value = true
                                    isMuted.value = false

                                } ) {
                                    Icon(modifier = Modifier.size(20.dp),painter = painterResource(id = R.drawable.ic_volume_slash), contentDescription = "", tint = Color(0xFF848484))
                                }

                            }else{
                                IconButton(onClick ={
                                    scope.launch {
                                        muteReport(reportId.value, context = context)
                                    }
                                    isChangeDetacted.value = true
                                    isMuted.value = true

                                } ) {
                                    Icon(modifier = Modifier.size(20.dp),painter = painterResource(id = R.drawable.volume_high), contentDescription = "", tint = Color(0xFF495CE8))
                                }

                            }
                        }
                        if (report.value.isReportOwner || vModel.currentUserTier.value == UserTiers.ADMIN){
                            Spacer(modifier = Modifier.width(5.dp))
                            if (vModel.isDeleteReportRequested.value){
                                Box(
                                    Modifier
                                        .size(42.dp)
                                , contentAlignment = Alignment.Center) {
                                    Loading(25)
                                }

                            }else{
                            Box(modifier = Modifier
                                .background(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.Transparent
                                )
                                .padding(horizontal = 3.dp, vertical = 6.dp)
                                .clickable {
                                    onReportDelete(reportId.value)
                                }) {

                                Icon(modifier = Modifier.size(20.dp),painter = painterResource(id = R.drawable.ic_trash),contentDescription = "", tint = Color.Unspecified)
                            }
                            }
                            Spacer(modifier = Modifier.width(5.dp))
                        }
//                    Icon(painter = painterResource(id = R.drawable.ic_share), contentDescription = "", tint = Color(0XFF848484))
                    }
                }
                AnimatedVisibility(visible = !isShowFeedBackPanel.value, enter = slideInVertically(), exit = slideOutVertically()) {
                    Column {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Box(modifier = Modifier.width(60.dp)){

                                ReportIconWithSpeedLimit(icon = reportUI.icon, speedLimit = localReport.value?.reportSpeedLimit, color = reportUI.color1)
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Column {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(modifier = Modifier.fillMaxWidth(),verticalAlignment = Alignment.CenterVertically) {
                                    if(isLoading.value){
                                        Row(
                                            Modifier
                                                .width(120.dp)
                                                .height(15.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .shimmerEffect()){}
                                    }else{
                                        Text(text ="${stringResource(id = R.string.lbl_by)}: ${if(reportOwnerInfo.value.userName.isBlank() || report.value.reportType > 4){ "Cee" }else{reportOwnerInfo.value.userName} }", fontSize = 16.sp,fontWeight = FontWeight.W600)
                                        if (reportOwnerInfo.value.userName.isBlank() || reportOwnerInfo.value.userType == 2 || reportOwnerInfo.value.userType == 1){
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(modifier = Modifier.size(18.dp),painter = painterResource(id = R.drawable.ic_verified_badge), contentDescription = "", tint = Color.Unspecified)
                                        }
                                    }

                                }
                                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
//                                stringResource(id = R.string.lbl_home_report_feedback_sheet_title_approve) +" "+ title.value
//                                Text(text = stringResource(id = R.string.lbl_home_report_feedback_sheet_description),fontWeight = FontWeight.W400,fontFamily = FontFamily(
//                                    Font(R.font.work_sans_medium)))
                                    Icon(painter = painterResource(id = R.drawable.ic_alerted_car_3x), contentDescription = "")
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(text = "${report.value.alertedCount} alerted", fontSize = 13.sp,fontWeight = FontWeight.W500)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(22.dp))
                        Row(modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(scrollState), horizontalArrangement = Arrangement.Start) {
                            localReport.value?.reportAddress?.let{
                                if (it != ""){
                                    Box(modifier = Modifier
                                        .height(36.dp)
                                        .background(
                                            color = Color(0XFFF7F7F7),
                                            shape = RoundedCornerShape(15.dp)
                                        ), contentAlignment = Alignment.Center) {
                                        Box(modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 10.dp), contentAlignment = Alignment.Center){
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(modifier = Modifier.size(18.dp),painter = painterResource(id = R.drawable.ic_bold_location), contentDescription = "", tint = Color(0XFFAAAAAA))
                                                Spacer(modifier = Modifier.width(5.dp))
                                                Text(text = it, maxLines = 1,overflow = TextOverflow.Ellipsis,fontWeight = FontWeight.W400)
                                            }
                                        }
                                        Log.d("SCROL","${scroll.maxValue}")
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                }

                            }

//                            Box(modifier = Modifier
//                                .height(40.dp)
//                                .background(
//                                    color = Color(0XFFF7F7F7),
//                                    shape = RoundedCornerShape(15.dp)
//                                ), contentAlignment = Alignment.Center) {
//                                Row(modifier = Modifier
//                                    .padding(horizontal = 10.dp)) {
//                                    Icon(modifier = Modifier.size(18.dp),painter = painterResource(id = icon.value), contentDescription = "",tint = Color(0XFFAAAAAA))
//                                    Spacer(modifier = Modifier.width(5.dp))
//                                    Text(text = title.value,fontWeight = FontWeight.W400)
//                                }
//
//                            }
//                            Spacer(modifier = Modifier.width(5.dp))
                            Box(modifier = Modifier
                                .height(36.dp)
                                .background(
                                    color = Color(0XFFF7F7F7),
                                    shape = RoundedCornerShape(15.dp)
                                ), contentAlignment = Alignment.Center) {
                                Row(modifier = Modifier
                                    .padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(modifier = Modifier.size(20.dp),painter = painterResource(id = R.drawable.ic_bold_location), contentDescription = "",tint = Color(0XFFAAAAAA))
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(text = "" +report.value.reportLocation?.let { df.format(it.distanceTo(vModel.lastLocation.value).div(1000)) } + " KM " + stringResource(id = R.string.lbl_home_report_feedback_sheet_tag_distanceAway), maxLines = 1,overflow = TextOverflow.Ellipsis,fontWeight = FontWeight.W400)
                                }

                            }
                        }
                        Spacer(modifier = Modifier.height(15.dp))
                        ReliabilityProgressRatio(report.value.feedbackLikeCount,report.value.feedbackDisLikeCount)

//                        Row(modifier = Modifier.fillMaxWidth()) {
//                            Text(text = "Reliably: ", fontSize = 16.sp, fontWeight = FontWeight.W500,color = Color(0xFF171729))
//                            var rel = ""
//                            if(likePercent.value >= 0.25f){
//                                rel = "Low"
//                            }else if(likePercent.value >= 0.5f){
//                                rel = "Medium"
//                            }else if(likePercent.value >= 0.75f){
//                                rel = "High"
//                            }else {
//                                rel = "Hery "
//                            }
//                            Text(text =rel,fontSize = 16.sp, fontWeight = FontWeight.W500,color = Color(0xFF495CE8))
//
//                        }
//                        Spacer(modifier = Modifier.height(14.dp))
//                        Row(modifier = Modifier.fillMaxWidth(),verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
//                            HorizontalProgress(progressAnimationValue)
//                            Spacer(modifier = Modifier.width(3.dp))
//                            Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
//                                Row(verticalAlignment = Alignment.CenterVertically) {
//                                    Icon(modifier = Modifier.size(16.dp),painter = painterResource(id = R.drawable.ic_like), tint = Color(0xFF57D654), contentDescription = "")
//                                    Text(text = "(${report.value.feedbackLikeCount})",fontSize = 14.sp, fontWeight = FontWeight.W600, color = Color(0xFF57D654))
//                                }
//                                Spacer(modifier = Modifier.width(3.dp))
//                                Row(verticalAlignment = Alignment.CenterVertically) {
//                                    Icon(modifier = Modifier.size(16.dp),painter = painterResource(id = R.drawable.ic_dislike), tint = Color(0xFFEA4E34), contentDescription = "")
//                                    Text(text = "(${report.value.feedbackDisLikeCount})",fontSize = 14.sp, fontWeight = FontWeight.W600, color = Color(0xFFEA4E34))
//                                }
//                            }
//                        }
                        
                        Spacer(modifier = Modifier.height(10.dp))

                        if(isShowFeedBack.value){
                            Divider(thickness = 2.dp, color = Color(0XFFF7F7F7))
                        }





                    }
                }

                if(isShowFeedBack.value){
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text ="${stringResource(id = R.string.lbl_home_report_feedback_sheet_title_approve)} ${reportUI.title}?",fontWeight = FontWeight.W700, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(text = stringResource(id = R.string.lbl_home_report_feedback_sheet_description),fontWeight = FontWeight.W400,fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(15.dp))


                    Row(modifier = Modifier.fillMaxWidth()) {

                        Box(modifier = Modifier.fillMaxWidth(0.5f)){
                            LikeButton(isLiked = if(isLike.value == true){true}else if(isLike.value == false){false}else{null}, likeCount =if (vModel.userInfo.value.userType == 2){ report.value.feedbackLikeCount}else{null}, likePresent = likePercent){
                                vModel.addReportFeedback(reportId.value,true)
                                coroutineScope.launch {
                                    vModel.getSingleReport(reportId = reportId.value).collect{
                                        if (it.isSuccess){
                                            report.value = it
                                            isLoading.value = false

                                            isLike.value = it.isLiked
                                            likePercent.value = 0.001f.coerceAtLeast (it.feedbackLikeCount.toFloat()/ (it.feedbackLikeCount.toFloat() + it.feedbackDisLikeCount.toFloat()))
                                            disLikePercent.value =0.001f.coerceAtLeast (it.feedbackDisLikeCount.toFloat()/(it.feedbackLikeCount.toFloat()+it.feedbackDisLikeCount.toFloat()))
                                        }
                                    }
                                }
//                        coroutineScope.launch {
//                            vModel.loadReportFeedbacks(reportId = reportId).collect{
//                                isLike.value = it.isLiked
//                                Log.d("FEEDBACKDEBUG", it.feedbackLikeCount.toString())
//                                Log.d("FEEDBACKDEBUG", it.feedbackDisLikeCount.toString())
//
//                                likePercent.value = (it.feedbackLikeCount/ (it.feedbackLikeCount + it.feedbackDisLikeCount).toFloat())
//                                disLikePercent.value = (it.feedbackDisLikeCount/(it.feedbackLikeCount+it.feedbackDisLikeCount).toFloat())
//                            }
//                        }
                            }
                        }
                        Spacer(modifier = Modifier.width(5.dp))
                        Box(modifier = Modifier.fillMaxWidth()){
                            DisLikeButton(isLiked = if(isLike.value == true){false}else if(isLike.value == false){true}else{null}, disLikePresent= disLikePercent,dislikeCount =if (vModel.userInfo.value.userType == 2){ report.value.feedbackDisLikeCount}else{null} ){
                                vModel.addReportFeedback(reportId.value,false)
                                coroutineScope.launch {
                                    vModel.getSingleReport(reportId = reportId.value).collect{
                                        if (it.isSuccess){
                                            report.value = it
                                            isLoading.value = false

                                            isLike.value = it.isLiked
                                            likePercent.value = 0.001f.coerceAtLeast (it.feedbackLikeCount.toFloat()/ (it.feedbackLikeCount.toFloat() + it.feedbackDisLikeCount.toFloat()))
                                            disLikePercent.value =0.001f.coerceAtLeast (it.feedbackDisLikeCount.toFloat()/(it.feedbackLikeCount.toFloat()+it.feedbackDisLikeCount.toFloat()))
                                        }
                                    }
                                }

//                        coroutineScope.launch {
//                            vModel.loadReportFeedbacks(reportId = reportId).collect{
//                                isLike.value = it.isLiked
//                                Log.d("FEEDBACKDEBUG", it.feedbackLikeCount.toString())
//                                Log.d("FEEDBACKDEBUG", it.feedbackDisLikeCount.toString())
//
//                                likePercent.value = (it.feedbackLikeCount/ (it.feedbackLikeCount + it.feedbackDisLikeCount).toFloat())
//                                disLikePercent.value = (it.feedbackDisLikeCount/(it.feedbackLikeCount+it.feedbackDisLikeCount).toFloat())
//                            }
//                        }

                            }
                        }
                    }

                    Spacer(modifier = Modifier.height((bottomNavigationBarHeight.value + 10).dp))
                }else{
                    Spacer(modifier = Modifier.height((bottomNavigationBarHeight.value + 10).dp))
                }





//                Button(onClick = { isLiked.value = true
//                    isShowFeedBackPanel.value = false
//                },
//                    enabled = report.value.reportType != 5 || report.value.reportType != 6,
//                    border = if (isLiked.value == true){
//                        BorderStroke(2.dp, color = Color(0XFF57D654))}else{null},
//                    elevation  = elevation(
//                        defaultElevation = 0.dp,
//                        pressedElevation = 0.dp),
//                    colors = ButtonDefaults.buttonColors(backgroundColor =if (isLiked.value == false){Color(0XFFE4E4E4)}else{Color(0XFFEEFBEE)} ),
//                    modifier = Modifier
//                        .fillMaxWidth(0.5f)
//                        .height(45.dp), shape = RoundedCornerShape(10.dp)) {
//                    Row(verticalAlignment = Alignment.CenterVertically) {
//                        Icon(painter = painterResource(id = R.drawable.ic_like), contentDescription = "",tint = if (isLiked.value == false){Color(0XFF848484)}else{Color(0XFF57D654)})
//                        Spacer(modifier = Modifier.width(5.dp))
//                        Text(text = "It is There",fontWeight = FontWeight.W600,fontFamily = FontFamily(
//                            Font(R.font.work_sans_medium)),color =if (isLiked.value == false){Color(0XFF848484)}else{Color(0XFF57D654)}
//                        )
//                    }
//
//
//                }
//                Spacer(modifier = Modifier.width(10.dp))
//                Button(onClick = { isLiked.value = false
//                    isShowFeedBackPanel.value = true
//                },
//                    enabled = report.value.reportType != 5 || report.value.reportType != 6,
//                    border = if (isLiked.value == false){
//                        BorderStroke(2.dp, color = Color(0XFFEA4E34))}else{ null},
//                    elevation  = elevation(
//                        defaultElevation = 0.dp,
//                        pressedElevation = 0.dp),
//                    colors = ButtonDefaults.buttonColors(
//                        backgroundColor =if (isLiked.value == true){Color(0XFFE4E4E4)}else{Color(0XFFFDEDEB)} ),
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .height(45.dp), shape = RoundedCornerShape(10.dp)) {
//                    Row(verticalAlignment = Alignment.CenterVertically) {
//                        Icon(painter = painterResource(id = R.drawable.ic_dislike), contentDescription = "", tint =if (isLiked.value == true){Color(0XFF848484)}else{Color(0XFFEA4E34)} )
//                        Spacer(modifier = Modifier.width(5.dp))
//                        Text(text = "Not There",fontWeight = FontWeight.W600,fontFamily = FontFamily(
//                            Font(R.font.work_sans_medium)), color = if (isLiked.value == true){Color(0XFF848484)}else{Color(0XFFEA4E34)})
//                    }
//
//                }



                AnimatedVisibility(visible = isShowFeedBackPanel.value, enter = slideInVertically(), exit = slideOutVertically()) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(text = "Disliked", fontSize = 22.sp,fontWeight = FontWeight.W900,fontFamily = FontFamily(
                            Font(R.font.work_sans_medium)))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(text = "What went worng?",fontWeight = FontWeight.W400,fontFamily = FontFamily(
                            Font(R.font.work_sans_medium)))
                        Spacer(modifier = Modifier.height(10.dp))

                        Box(modifier = Modifier
                            .fillMaxWidth()
                            .height(155.dp)
                            .border(
                                border = BorderStroke(width = 2.dp, color = Color(0XFFE4E4E4)),
                                shape = RoundedCornerShape(10.dp)
                            )){
                            Column(modifier = Modifier.fillMaxSize()) {
                                Row(modifier = Modifier
                                    .fillMaxWidth()
                                    .pointerInput(Unit) {
                                        detectTapGestures(onTap = {
                                            dislikeReason.value = 1
                                            isError.value = false
                                        })
                                    }
                                    .padding(horizontal = 15.dp)
                                    .height(50.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = "choice one")
                                    radio(dislikeReason.value == 1,errorColor = if (isError.value){Color(0XFFEA4E34)}else{Color(0XFF707070)})

                                }
                                Divider(color = Color(0XFFE4E4E4), thickness = 2.dp)
                                Row(modifier = Modifier
                                    .pointerInput(Unit) {
                                        detectTapGestures(onTap = {
                                            dislikeReason.value = 2
                                            isError.value = false
                                        })
                                    }
                                    .fillMaxWidth()
                                    .padding(horizontal = 15.dp)
                                    .height(50.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = "choice two")
                                    radio(dislikeReason.value == 2,errorColor = if (isError.value){Color(0XFFEA4E34)}else{Color(0XFF707070)})

                                }
                                Divider(color = Color(0XFFE4E4E4), thickness = 2.dp)
                                Row(modifier = Modifier
                                    .pointerInput(Unit) {
                                        detectTapGestures(onTap = {
                                            dislikeReason.value = 3
                                            isError.value = false
                                        })
                                    }
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .padding(horizontal = 15.dp)
                                    , horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = "choice three")
                                    radio(dislikeReason.value == 3, errorColor = if (isError.value){Color(0XFFEA4E34)}else{Color(0XFF707070)})

                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            Text(text = "Leave a comment", color = Color(0XFF495CE8))

                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            Button(modifier = Modifier
                                .height(55.dp)
                                .fillMaxWidth(), shape = RoundedCornerShape(10.dp),
                                onClick = {
                                    if(dislikeReason.value == null){
                                        isError.value = true
                                    }
                                }, colors = ButtonDefaults.buttonColors(backgroundColor = Color(0XFF495CE8) )) {

                                Text(text = "Submit", color = Color.White, fontWeight = FontWeight.Bold)
                            }


                        }
                    }
                }

            }
        }


    }
}
fun Modifier.shimmerEffect(): Modifier = composed {
    var size by remember {
        mutableStateOf(IntSize.Zero)
    }
    val transition = rememberInfiniteTransition()
    val startOffsetX by transition.animateFloat(
        initialValue = -2 * size.width.toFloat(),
        targetValue = 2 * size.width.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2000)
        )
    )

    background(
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0xFFB8B5B5),
                Color(0xFF8F8B8B),
                Color(0xFFB8B5B5),
            ),
            start = Offset(startOffsetX, 0f),
            end = Offset(startOffsetX + size.width.toFloat(), size.height.toFloat())
        )
    )
        .onGloballyPositioned {
            size = it.size
        }
}
private suspend fun muteReport(el: String, context: Context){
    context.data_Store.updateData {appSettings->
        val tempL : ArrayList<String> = arrayListOf()
        tempL.addAll(appSettings.mutedReports)
        if (!tempL.contains(el)){
            tempL.add(el)
            MyLocationService.LSR.allMutedReports.addAll(tempL)
        }
        appSettings.copy(mutedReports = tempL)
    }
}
private suspend fun unMuteReport(el: String, context: Context){
    context.data_Store.updateData {appSettings->
        val tempL : ArrayList<String> = arrayListOf()
        tempL.addAll(appSettings.mutedReports)
        tempL.remove(el)
        MyLocationService.LSR.allMutedReports.addAll(tempL)
        appSettings.copy(mutedReports = tempL)
    }
}

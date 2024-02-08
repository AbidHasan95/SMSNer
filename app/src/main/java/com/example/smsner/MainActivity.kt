package com.example.smsner

import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Telephony
import androidx.activity.ComponentActivity
//import androidx.compose.material3.
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smsner.ui.theme.SMSNERTheme
import com.example.smsner.utils.SMSMessage
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var mymodel:NERModel = NERModel(this)
        setContent {
            SMSNERTheme {
                // A surface container using the 'background' color from the theme
//                https://www.tutorialspoint.com/how-to-request-location-permission-at-runtime-on-kotlin
//                https://developer.android.com/training/permissions/requesting
//                https://stackoverflow.com/questions/58741279/kotlin-app-not-asking-user-for-permissions
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
//                    Greeting("Android", model = mymodel)
                    showUI(model = mymodel)
                }
            }
        }
    }
    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            1 -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() &&
                            grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // Permission is granted. Continue the action or workflow
                    // in your app.
                } else {
                    // Explain to the user that the feature is unavailable because
                    // the feature requires a permission that the user has denied.
                    // At the same time, respect the user's decision. Don't link to
                    // system settings in an effort to convince the user to change
                    // their decision.
                }
                return
            }

            // Add other 'when' lines to check for other
            // permissions this app might request.
            else -> {
                // Ignore all other requests.
            }
        }
    }
}

@Composable
fun showUI(model: NERModel?) {
//    https://stackoverflow.com/questions/72832802/how-to-show-multiple-color-text-in-same-text-view-with-jetpack-compose
//    https://medium.com/geekculture/add-remove-in-lazycolumn-list-aka-recyclerview-jetpack-compose-7c4a2464fc9f
//    https://developer.android.com/jetpack/compose/lists
//    https://foso.github.io/Jetpack-Compose-Playground/foundation/lazycolumn/
    var msgList = remember {
//        mutableStateListOf<MutableList<String>>()
        mutableStateListOf<SMSMessage>()
    }
    val msgList2 = remember {
        mutableListOf<String>()
    }
    val colorMap1 = mapOf(
        "O" to Color(0xFF328FB1),
        "CREDIT" to Color(0xFF328FB1),
        "EXPIRY" to Color(0xFFF27B77),
        "COURIER_SERVICE" to Color(0xFFBBD6FD),
        "OTP" to Color(0xFF328FB1),
        "TRACKING_URL" to Color(0xFFDFACF6),
        "DEBIT" to Color(0xFFF27877),
        "TRACKING_ID" to Color(0xFFD9C8F2),
        "REFUND" to Color(0xFFD6E286)
    )
    msgList2.add("test 1")
    msgList2.add("Test 2")
    var selectedDate1 = remember { mutableStateOf(1L) }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            showTopContent(selectedDate1, msgList,model!!)
        }
        LazyColumn(
            modifier= Modifier
                .fillMaxSize()
        ) {
//            items(msgList) {message ->
//                Text(text = msgList[index].toString())
//            }
//            items(msgList) { item ->
            itemsIndexed(msgList) { index,item ->
                val annotatedlabels1 = mutableSetOf<String>()
                Card(
                    modifier = Modifier.padding(top = 5.dp, start = 5.dp, end = 5.dp)
                ) {

                    Text(
                        modifier = Modifier
                            .padding(10.dp)
                            .fillMaxWidth(),
                            text=buildAnnotatedString {
                                for((token,label) in item.msgWords) {
                                    if (label!="O") {
                                        withStyle(SpanStyle(color = colorMap1.getOrElse(label, { Color.Blue }))) {
                                            annotatedlabels1.add(label)
                                            append(token)
                                        }
                                    }
                                    else {
                                        append(token)
                                    }
                                    append(" ")
                                }
                                toAnnotatedString()
                            }
                    )
                    if (annotatedlabels1.size>0) {
                        Divider(
                            modifier = Modifier.padding(horizontal = 5.dp),
                            thickness = 0.5.dp,
                            color = Color.Black
                        )
                        Text(
                            modifier = Modifier.padding(top = 8.dp, start = 8.dp, bottom = 8.dp, end = 8.dp),
                            fontSize = 12.sp,
                            text=buildAnnotatedString{
                                append(item.msgSender)
                                append("\n"+item.msgDate)
                                for(x in annotatedlabels1) {
                                    withStyle(SpanStyle(color = colorMap1.getOrElse(x, { Color.Blue }))) {
                                        append("\n⬤ ")
                                    }
                                    append(x)
                                }
                                toAnnotatedString()
                            }
                        )
                    }
                }
            }
        }
    }
}


fun readSMS(
    msgList: MutableList<SMSMessage>,
    context: Context,
    selectedDate1: String,
    startTime: Long,
    endTime: Long
) {
//    https://stackoverflow.com/questions/9713021/reading-sms-received-after-a-date
//    https://www.tutorialspoint.com/how-can-i-read-sms-messages-from-the-device-programmatically-in-android
//    https://github.com/stevdza-san/ReadSMSDemo/blob/master/app/src/main/java/com/stevdza/san/readsmsdemo/MainActivity.kt
    msgList.clear()
    val dateFilter = "date>=$startTime and date<=$endTime"
    val cursor = context.contentResolver.query(
//        Uri.parse("content://sms/inbox"),
        Telephony.Sms.CONTENT_URI,
        null,
        dateFilter,
        null,
        null,
    )
//    if (cursor!=null) {
//        if (cursor!!.moveToFirst()) {
//            val message = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.BODY))
//            msgList.add(message.toString())
//            cursor.moveToNext()
//        }
//    }

    if (cursor != null) {
        while(cursor.moveToNext()) {
            val message = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.BODY))
            val origTokens = message!!.trim().split("\\s+".toRegex())
            val temp = mutableListOf<MutableList<String>>()
            var msgDate = cursor.getString(cursor.getColumnIndexOrThrow((Telephony.Sms.DATE)))
            var msgSender = cursor.getString(cursor.getColumnIndexOrThrow((Telephony.Sms.ADDRESS)))
            for (token in origTokens) {
                temp.add(mutableListOf(token,"O"))
            }
            val smsObj = SMSMessage(msgWords = temp,msgDate = msgDate, msgSender = msgSender)
            msgList.add(smsObj)
        }
    }
//    cursor!!.close()
}

@Composable
fun messageUI() {
    val a = "Test"
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun mydatepicker(
    state: DatePickerState, openDialog: MutableState<Boolean>,
    selectedDate1: MutableState<Long>,
    msgList: MutableList<SMSMessage>
) {
//    https://medium.com/mobile-app-development-publication/date-and-time-picker-with-compose-9cadc4f50e6d
//    https://medium.com/@rahulchaurasia3592/material3-datepicker-and-datepickerdialog-in-compose-in-android-54ec28be42c3
//    https://www.geeksforgeeks.org/datepicker-in-kotlin/
    val c = Calendar.getInstance()
    val context = LocalContext.current
    val calStatetemp = rememberDatePickerState(c.timeInMillis, initialDisplayMode = DisplayMode.Picker)
    val simpleDateFormat = SimpleDateFormat("yyyy-mm-dd")
    val simpleDateFormat2 = SimpleDateFormat("yyyy-mm-dd'T'HH:mm:ss")
    return(
        if (openDialog.value) {
            DatePickerDialog(
                onDismissRequest = {
                    openDialog.value = false
                },
                dismissButton = {
                    TextButton(onClick = {
                        openDialog.value = false
                    }) {
                        Text(text = "Cancel")
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        openDialog.value = false
                        selectedDate1.value = calStatetemp.selectedDateMillis!!
                        val mydate = simpleDateFormat.format(Date(selectedDate1.value) )
//                        val startTime = mydate+"T00:00:00"
                        val startTime = simpleDateFormat2.parse(mydate+"T00:00:00")!!.time
//                        val endTime = mydate+"T23:59:59"
                        val endTime = simpleDateFormat2.parse(mydate+"T23:59:59")!!.time

                        readSMS(msgList,context,mydate,startTime,endTime)
                    }) {
                        Text(text = "Ok")
                    }
            }) {
                DatePicker(state = calStatetemp)
            }
        } else {

        })
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun showTopContent(
    selectedDate1: MutableState<Long>,
    msgList: MutableList<SMSMessage>,
    model: NERModel
) {
    val c = Calendar.getInstance()

//    var selectedDate1 = remember { mutableStateOf(1L) }
    val openDialog = remember { mutableStateOf(false)}
    if (selectedDate1.value !=1L) {
        
    }
    val calState = rememberDatePickerState(c.timeInMillis, initialDisplayMode = DisplayMode.Picker)
//    Text(text = calState.selectedDateMillis.toString())
//    Text(text = selectedDate1.value.toString())
    Text(text = msgList.size.toString()+" -> "+selectedDate1.value)
    Button(onClick = {
//        val newFragment = DatePickerFragment()
        openDialog.value = true
    }) {
        Text(text="Pick Date")
    }
    mydatepicker(state = calState, openDialog = openDialog, selectedDate1, msgList)

//    DatePicker(state = calState, headline = null, title = null)
    Button(onClick = {
        model.predict2(msgList)
    }) {
        Text(text = "Predict")
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier, model: NERModel) {
//    var output2 = model?.predict("Dear Customer, INR 35,535.02 is debited on ICICI Bank Credit Card XX3005 on 27-Oct-21. Info: Amazon 6 EMI. Available Limit: INR 1,18,929.96.")
    var output by remember {
        mutableStateOf("")
    }
    var textInput by remember {
        mutableStateOf("")
    }

    Column {
        TextField(value = textInput, onValueChange = { textInput = it }, label = { Text(text = "Input Text")})
        Button(onClick = {
//            if(textInput.length>0) {
//                var output2 = model?.predict(textInput)
//                output = output2!!.joinToString("\t")
//            }
        }) {
            Text(text = "Predict")
        }
        Text(
            text = output,
            modifier = modifier
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    SMSNERTheme {
//        Greeting("Android", model = null)
        showUI(model =null)
    }
}
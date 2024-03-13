package com.example.smsner.utils

class SMSMessage(
    msgWords: MutableList<MutableList<String>>,
    msgDate: String,
    msgSender: String,
    msgEpochTime: Long
) {
    var smsCategory: String = ""
    var msgSender: String
    var msgDate: String
    var msgWords: MutableList<MutableList<String>>
    var msgEpochTime: Long

    init {
        this.msgWords = msgWords
        this.msgDate = msgDate
        this.msgSender = msgSender
        this.msgEpochTime = msgEpochTime
    }
}
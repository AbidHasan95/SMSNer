package com.example.smsner.utils

class SMSMessage(msgWords: MutableList<MutableList<String>>, msgDate: String, msgSender:String) {
    var msgSender: String
    var msgDate: String
    var msgWords: MutableList<MutableList<String>>

    init {
        this.msgWords = msgWords
        this.msgDate = msgDate
        this.msgSender = msgSender
    }
}
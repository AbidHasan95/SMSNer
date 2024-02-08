package com.example.smsner

import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import com.example.smsner.utils.SMSMessage
import org.tensorflow.lite.Interpreter
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.nio.channels.FileChannel

class NERModel(context: Context):AutoCloseable {
    private var tflite: Interpreter? = null
    private val MODEL_PATH = "model.tflite"
    private val DIC_PATH = "vocab.txt"
    private val MAX_SEQ_LEN = 512
    private val NUM_CLASSES = 9
    private val DO_LOWER_CASE = true
    private val dict1 = mutableMapOf<String,Int>();
    private val featureConverter = FeaureConverterNer(dict1, DO_LOWER_CASE,MAX_SEQ_LEN)


    init {
//        var metadtaextractor = MetadataExtractor
        loadDictionaryFile(context.assets)
        loadModelFile(context.assets)
    }

    @Throws(IOException::class)
    fun loadDictionaryFile(assetManager: AssetManager) {
        assetManager.open(DIC_PATH).use { ins ->
            BufferedReader(InputStreamReader(ins)).use { reader ->
                var index = 0
                while (reader.ready()) {
                    val key:String = reader.readLine()
                    dict1[key] = index++

                }
            }
        }
    }

    @Throws(IOException::class)
    private fun loadModelFile(assetManager: AssetManager) {
        val assetFileDescriptor = assetManager.openFd(MODEL_PATH)
        val fileInputStream = FileInputStream(assetFileDescriptor.getFileDescriptor())
        val fileChannel = fileInputStream.getChannel()
        val startoffset = assetFileDescriptor.getStartOffset()
        val declaredLength = assetFileDescriptor.getDeclaredLength()
//        val opt = Interpreter.Options()
        val buffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startoffset, declaredLength)
//        var metadataExtractor = MetadataExtractor(buffer)
//        loadDictionaryFile(metadataExtractor!!.getAssociatedFile(DIC_PATH))
        tflite = Interpreter(buffer)

    }

    fun predict(msgList: MutableList<SMSMessage>) {
//        https://stackoverflow.com/questions/65021800/tensorflow-lite-select-tf-ops-and-tensorflow-lite-aar-built-from-source-tflite-m
//        https://www.tensorflow.org/lite/android/lite_build
//        https://repo1.maven.org/maven2/org/tensorflow/tensorflow-lite/
//        https://central.sonatype.com/artifact/org.tensorflow/tensorflow-lite
//        https://farmaker47.medium.com/build-tensorflow-lite-select-tf-ops-aar-and-tensorflow-lite-aar-files-with-colab-a2a6603602c8
        val numMsgs = msgList.size
        for ((msgIndex,msgTemp) in msgList.withIndex()) {
            val outputMap = mutableMapOf<Int,Array<Array<FloatArray>>>()
            val origIndexMap = mutableMapOf<Int,Any>()
            val inputIds = Array(1) { LongArray(MAX_SEQ_LEN)}
            val tokenTypeIds = Array(1) { LongArray(MAX_SEQ_LEN)}
            val attentionMask = Array(1) { LongArray(MAX_SEQ_LEN)}
            val output2 = Array(1) { Array(MAX_SEQ_LEN) { FloatArray(NUM_CLASSES) } }

            val msgTokens = msgList.get(msgIndex).msgWords.map { it[0] }
            var feature: Feature? = featureConverter.convert(msgTokens)
            val origIndex = feature!!.tokenToOrigIndex
            var tokens = feature!!.tokens
            origIndexMap.put(msgIndex,origIndex)
            for (i in 0 until MAX_SEQ_LEN) {
                inputIds[0][i] = feature!!.inputIds[i]
                tokenTypeIds[0][i] = feature!!.tokenTypeIds[i]
                attentionMask[0][i] = feature!!.attentionMask[i]
//            outputs2[i] = floatArrayOf(0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f)
                for (j in 0 until NUM_CLASSES) {
                    output2[0][i][j] = 0f
                }
            }
            val inputs = arrayOf(attentionMask, inputIds, tokenTypeIds)
            outputMap.put(0, output2)
            tflite?.runForMultipleInputsOutputs(inputs, outputMap as Map<Int, Any>)
            Log.v("DEBUG","run successful")
            getNERLabels(outputMap,origIndex,msgList,msgIndex,msgTokens,tokens)
        }


//        inputs[0] = inputIds
//        inputs[1] = tokenTypeIds
//        inputs[2] = attentionMask
//        3 input(s):
//        [  1 512] <class 'numpy.int64'>
//        [  1 512] <class 'numpy.int64'>
//        [  1 512] <class 'numpy.int64'>
//
//        1 output(s):
//        [  1 512   9] <class 'numpy.float32'>
        var inputTensorCount =  tflite?.inputTensorCount
        var outputcount = tflite?.outputTensorCount
        var inp_shape = tflite?.getInputTensor(0)?.shape()
        var inp1_shape = tflite?.getInputTensor(0)?.name()
        var inp2_shape = tflite?.getInputTensor(1)?.name()
        var inp3_shape = tflite?.getInputTensor(2)?.name()
        var inp_dtype = tflite?.getInputTensor(0)?.dataType()
        var out1 = tflite?.getOutputTensor(0)?.shape()
        var out1_dtype = tflite?.getOutputTensor(0)?.dataType()
    }

    fun getNERLabels(
        tfoutput: MutableMap<Int, Array<Array<FloatArray>>>,
        origIndex: MutableList<Int>,
        msgList: MutableList<SMSMessage>,
        msgIndex: Int,
        msgTokens: List<String>,
        tokens: MutableList<String>
    ) {
        var label_list = arrayOf(
            "O",
            "CREDIT",
            "EXPIRY",
            "COURIER_SERVICE",
            "OTP",
            "TRACKING_URL",
            "DEBIT",
            "TRACKING_ID",
            "REFUND"
        )
        val wordLabels = mutableMapOf<Int, String>()
        for (i in 1 until tokens.size) {
            if (tokens[i]=="[SEP]") {
                break
            }
            var maxidx = tfoutput[0]!![0][i]!!.indexOfFirst { it == tfoutput[0]!![0][i].max() }
            var label = label_list[maxidx]
//            var percent = tfoutput[0]!![0][i].max() / tfoutput[0]!![0][i].sum() * 100
            val wordIdx = origIndex[i-1]
            if ((wordLabels[wordIdx] == "O" && label!="O") || wordLabels[wordIdx] == null) {
                wordLabels[wordIdx] = label
                msgList[msgIndex].msgWords[wordIdx][1] = label
            }
        }
    }

    override fun close() {
        if (tflite != null) {
            tflite!!.close()
            tflite = null
        }
        dict1.clear()
    }
}
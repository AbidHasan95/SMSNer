package com.example.smsner

import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import com.example.smsner.utils.SMSMessage
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.nio.channels.FileChannel

class NERModel(context: Context):AutoCloseable {
    private var tflite: Interpreter? = null
//    private val MODEL_PATH = "model.tflite"
    private val MODEL_PATH = "model6-1.tflite"
//    private val MODEL_PATH = "google-bert_uncased_L-2_H-512_A-8-Adam-recommended-100E-4.tflite"
    private val DIC_PATH = "vocab.txt"
    private val MAX_SEQ_LEN = 512
    private val NUM_CLASSES = 16
    private val DO_LOWER_CASE = true
    private val dict1 = mutableMapOf<String,Int>();
    private val featureConverter = FeaureConverterNer(dict1, DO_LOWER_CASE,MAX_SEQ_LEN)
    private val context: Context = context

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
        val compatList = CompatibilityList()
        val opt = Interpreter.Options().apply{
//            if(compatList.isDelegateSupportedOnThisDevice){
//                // if the device has a supported GPU, add the GPU delegate
//                val delegateOptions = compatList.bestOptionsForThisDevice
//                this.addDelegate(GpuDelegate(delegateOptions))
//            } else {
//                // if the GPU is not supported, run on 4 threads
//                this.setNumThreads(4)
//            }
            this.setNumThreads(4)
        }
        val buffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startoffset, declaredLength)
//        var metadataExtractor = MetadataExtractor(buffer)
//        loadDictionaryFile(metadataExtractor!!.getAssociatedFile(DIC_PATH))
        tflite = Interpreter(buffer,opt)

    }

    fun predict(msgList: MutableList<SMSMessage>) {
//        https://stackoverflow.com/questions/65021800/tensorflow-lite-select-tf-ops-and-tensorflow-lite-aar-built-from-source-tflite-m
//        https://www.tensorflow.org/lite/android/lite_build
//        https://repo1.maven.org/maven2/org/tensorflow/tensorflow-lite/
//        https://central.sonatype.com/artifact/org.tensorflow/tensorflow-lite
//        https://farmaker47.medium.com/build-tensorflow-lite-select-tf-ops-aar-and-tensorflow-lite-aar-files-with-colab-a2a6603602c8
        var inputTensorCount =  tflite?.inputTensorCount
        var outputcount = tflite?.outputTensorCount
        var inp_shape = tflite?.getInputTensor(0)?.shape()
        var inp1_shape = tflite?.getInputTensor(0)?.name()
        var inp2_shape = tflite?.getInputTensor(1)?.name()
        var inp3_shape = tflite?.getInputTensor(2)?.name()
        var inp_dtype = tflite?.getInputTensor(0)?.dataType()
        var out1 = tflite?.getOutputTensor(0)?.shape()
        var out2 = tflite?.getOutputTensor(1)?.shape()
        var out1_dtype = tflite?.getOutputTensor(0)?.dataType()
        var out2_dtype = tflite?.getOutputTensor(1)?.dataType()
        val numMsgs = msgList.size
        for ((msgIndex,msgTemp) in msgList.withIndex()) {
//            val outputMap = mutableMapOf<Int,Array<Array<FloatArray>>>()
            val outputMap = mutableMapOf<Int,Any>()
            val origIndexMap = mutableMapOf<Int,Any>()
            val inputIds = Array(1) { LongArray(MAX_SEQ_LEN)}
            val tokenTypeIds = Array(1) { LongArray(MAX_SEQ_LEN)}
            val attentionMask = Array(1) { LongArray(MAX_SEQ_LEN)}
            val output1 = Array(1) { arrayOf(0f,0f,0f,0f,0f) }
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
            val output1_new = TensorBuffer.createFixedSize(intArrayOf(1,5),DataType.FLOAT32)
            val output2_new = TensorBuffer.createFixedSize(intArrayOf(1,512,9),DataType.FLOAT32)
//            outputMap.put(0, output1_new.buffer)
//            outputMap.put(1, output2_new.buffer)
//            ---------- Inference using tflite support library - start --------------
//            var model = GoogleBertUncasedL2H512A8AdamRecommended100e4.newInstance(context)
//
//            var byteBuffer_0 =  attentionMask[0].foldIndexed(ByteArray(attentionMask[0].size)) { i, a, v -> a.apply { set(i, v.toByte()) } }
//            var byteBuffer_1 =  inputIds[0].foldIndexed(ByteArray(inputIds[0].size)) { i, a, v -> a.apply { set(i, v.toByte()) } }
//            var byteBuffer_2 =  tokenTypeIds[0].foldIndexed(ByteArray(tokenTypeIds[0].size)) { i, a, v -> a.apply { set(i, v.toByte()) } }
//
//            val inputTensor_0 = TensorBuffer.createFixedSize(intArrayOf(512),DataType.FLOAT32)
//            val bytes_0 = ByteBuffer.wrap(byteBuffer_0)
//            inputTensor_0.loadBuffer(bytes_0)
//
//            val inputTensor_1 = TensorBuffer.createFixedSize(intArrayOf(512),DataType.FLOAT32)
//            val bytes_1 = ByteBuffer.wrap(byteBuffer_1)
//            inputTensor_1.loadBuffer(bytes_1)
//
//            val inputTensor_2 = TensorBuffer.createFixedSize(intArrayOf(512),DataType.FLOAT32)
//            val bytes_2 = ByteBuffer.wrap(byteBuffer_2)
//            inputTensor_2.loadBuffer(bytes_2)
//
//            val outputs = model.process(inputTensor_0,inputTensor_1,inputTensor_2)
//            val output_0 = outputs.outputFeature0AsTensorBuffer
//            val output_1 = outputs.outputFeature1AsTensorBuffer
//            model.close()
//            --------- Inference using tflite support library - end ---------

//            val output1_new_1 = TensorBuffer.createFixedSize(intArrayOf(1,512),DataType.STRING)
//            val output2_new_1 = TensorBuffer.createFixedSize(intArrayOf(1),DataType.STRING)
            val output1_new_1 = arrayOf("")
            var output2_new_1 = arrayOf( Array<String>(512, { _ -> "" }))
            outputMap.put(0, output2_new_1)
            outputMap.put(1, output1_new_1)
            tflite?.runForMultipleInputsOutputs(inputs, outputMap as Map<Int, Any>)
//            val output1_floatArray = output1_new.floatArray
//            val output2_floatArray = output2_new.floatArray
            Log.v("Debug",output1_new_1.toString())
            Log.v("DEBUG","run successful")
//            getNERLabels(outputMap[1],origIndex,msgList,msgIndex,msgTokens,tokens)
            getNERLabels_new(output2_new_1[0],origIndex,msgList,msgIndex,msgTokens,tokens,output1_new_1[0])
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
//        var inputTensorCount =  tflite?.inputTensorCount
//        var outputcount = tflite?.outputTensorCount
//        var inp_shape = tflite?.getInputTensor(0)?.shape()
//        var inp1_shape = tflite?.getInputTensor(0)?.name()
//        var inp2_shape = tflite?.getInputTensor(1)?.name()
//        var inp3_shape = tflite?.getInputTensor(2)?.name()
//        var inp_dtype = tflite?.getInputTensor(0)?.dataType()
//        var out1 = tflite?.getOutputTensor(0)?.shape()
//        var out1_dtype = tflite?.getOutputTensor(0)?.dataType()
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

    fun getNERLabels_new(
        pred_labels: Array<String>,
        origIndex: MutableList<Int>,
        msgList: MutableList<SMSMessage>,
        msgIndex: Int,
        msgTokens: List<String>,
        tokens: MutableList<String>,
        smsCategory: String
    ) {
        val wordLabels = mutableMapOf<Int, String>()
        msgList[msgIndex].smsCategory = smsCategory
        for (i in 1 until tokens.size) {
            if (tokens[i]=="[SEP]") {
                break
            }
            var label = pred_labels[i]
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
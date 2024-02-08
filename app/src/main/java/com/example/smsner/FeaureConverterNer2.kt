package com.example.smsner

import java.util.Collections

class FeaureConverterNer2(inputDic: MutableMap<String, Int>, doLowerCase: Boolean, var maxSeqLen: Int) {

    var maxQueryLen:Int=0

    private var tokenizer: FullTokenizer

    init {
        this.tokenizer = FullTokenizer(inputDic, doLowerCase)
        this.maxQueryLen = maxQueryLen
    }

    fun convert(origTokens: List<String>): Feature? {

//        val origTokens = query!!.trim().split("\\s+".toRegex())
        val tokenToOrigIndex = mutableListOf<Int>()
        var allTokens = mutableListOf("[CLS]")
        for (i in origTokens.indices) {
            val token = origTokens[i]
            val subTokens = tokenizer!!.tokenize(token)
            for (subToken in subTokens) {
                tokenToOrigIndex.add(i)
                allTokens.add(subToken)
            }
        }
        if (allTokens.size > maxSeqLen) {
            allTokens = allTokens.subList(0, maxSeqLen-1)
        }
        allTokens.add("[SEP]")

        val inputIds = tokenizer!!.convertTokensToIds(allTokens)
        val tokenTypeIds: MutableList<Long> = ArrayList(Collections.nCopies(inputIds.size, 0L))
        val attentionMask: MutableList<Long> = ArrayList(Collections.nCopies(inputIds.size, 1L))
        while (inputIds.size < maxSeqLen) {
            inputIds.add(0L)
            tokenTypeIds.add(0L)
            attentionMask.add(0L)
        }
        return Feature(inputIds,tokenTypeIds, attentionMask,allTokens,tokenToOrigIndex)
    }
}

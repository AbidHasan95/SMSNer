package com.example.smsner

import java.util.Collections

class FeaureConverterNer(inputDic: MutableMap<String, Int>, doLowerCase: Boolean, var maxSeqLen: Int) {

    var maxQueryLen:Int=0

    private var tokenizer: FullTokenizer? = null
//    private val maxQueryLen = 0
//    private val maxSeqLen = 0

    init {
        this.tokenizer = FullTokenizer(inputDic, doLowerCase)
        this.maxQueryLen = maxQueryLen
    }

    fun convert(query: String?): Feature? {
        var queryTokens = tokenizer!!.tokenize(query)
        if (queryTokens.size > maxSeqLen) {
            queryTokens = queryTokens.subList(0, maxSeqLen)
        }
        val tokenToOrigIndex = mutableListOf<Int>()
        val tokens: MutableList<String?> = ArrayList()
        // Map token index to original index (in feature.origTokens).
        val tokenToOrigMap: MutableMap<Int, Int> = HashMap()

        // Start of generating the features.
        tokens.add("[CLS]")

        // For query input.
        for (queryToken in queryTokens) {
            tokens.add(queryToken)
        }
        // For Separation.
        tokens.add("[SEP]")

        // For ending mark.
        val inputIds = tokenizer!!.convertTokensToIds(tokens)
        val tokenTypeIds: MutableList<Long> = ArrayList(Collections.nCopies(inputIds.size, 0L))
        val attentionMask: MutableList<Long> = ArrayList(Collections.nCopies(inputIds.size, 1L))
        while (inputIds.size < maxSeqLen) {
            inputIds.add(0L)
            tokenTypeIds.add(0L)
            attentionMask.add(0L)
        }
        return Feature(inputIds, tokenTypeIds, attentionMask, tokens, tokenToOrigIndex)
    }
}

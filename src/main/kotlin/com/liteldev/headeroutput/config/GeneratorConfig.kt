package com.liteldev.headeroutput.config

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File

object GeneratorConfig {
    @OptIn(ExperimentalSerializationApi::class)
    private val json = Json { explicitNulls = false }

    lateinit var jsonPath: String
    lateinit var generatePath: String
    lateinit var configPath: String
    lateinit var generationExcludeRegexList: MutableList<String>
    lateinit var inclusionExcludeRegexList: MutableList<String>

    private lateinit var generatorConfigData: GeneratorConfigData


    fun loadConfig() {
        println("Loading config...")
        val configText = File(configPath).readText()
        generatorConfigData = json.decodeFromString(configText)
        generationExcludeRegexList = generatorConfigData.exclusion.generation.regex
        inclusionExcludeRegexList = generatorConfigData.exclusion.inclusion.regex
    }

    fun isExcludedFromGeneration(name: String): Boolean {
        return generationExcludeRegexList.any { name.matches(it.toRegex()) }
    }
}

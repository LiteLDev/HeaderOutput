package com.liteldev.headeroutput.config

import cc.ekblad.toml.decode
import cc.ekblad.toml.tomlMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.system.exitProcess

object GeneratorConfig {
    private val logger = KotlinLogging.logger { }

    @OptIn(ExperimentalSerializationApi::class)
    private val json = Json { explicitNulls = false }

    var enableRelativePath = false
    lateinit var rootPath: String
    lateinit var jsonPath: String
    lateinit var generatePath: String
    lateinit var configPath: String
    lateinit var declareMapPath: String
    lateinit var predefineHeaderPath: String
    lateinit var generationExcludeRegexList: List<String>
    lateinit var inclusionExcludeRegexList: List<String>

    private lateinit var generatorConfigData: OutputConfig


    fun loadConfig() {
        logger.info { "Loading config..." }
        val configText = File(configPath).readText()
        generatorConfigData = try {
            tomlMapper { }.decode(configText)
        } catch (e: Exception) {
            try {
                json.decodeFromString(configText)
            } catch (e: Exception) {
                logger.error { "Invalid config file" }
                exitProcess(1)
            }
        }
        enableRelativePath = generatorConfigData.config.enableRelativePath
        rootPath = generatorConfigData.config.rootPath
        generationExcludeRegexList = generatorConfigData.exclusion.generation.regex
        inclusionExcludeRegexList = generatorConfigData.exclusion.inclusion.regex
    }

    fun isExcludedFromGeneration(name: String): Boolean {
        return generationExcludeRegexList.any { name.matches(it.toRegex()) }
    }

    fun getSortRules() = generatorConfigData.sort

}

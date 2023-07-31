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
    lateinit var replacementRegex: List<Pair<Regex, String>>

    private lateinit var excludeRegexList: List<Regex>
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
        excludeRegexList = generatorConfigData.exclusion.regex.map { it.toRegex() }
        replacementRegex = generatorConfigData.replacement.regex.map { it.regex.toRegex() to it.to }
    }

    fun isExcludedFromGeneration(name: String): Boolean {
        return excludeRegexList.any { name.matches(it) }
    }

    fun getSortRules() = generatorConfigData.sort

}

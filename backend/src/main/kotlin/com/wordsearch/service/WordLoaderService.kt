package com.wordsearch.service

import com.opencsv.CSVReader
import com.wordsearch.model.Word
import com.wordsearch.repository.CategoryRepository
import com.wordsearch.repository.WordRepository
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.core.io.ResourceLoader
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.InputStreamReader

@Service
class WordLoaderService(
    private val wordRepository: WordRepository,
    private val categoryRepository: CategoryRepository,
    private val resourceLoader: ResourceLoader
) {

    private val logger = LoggerFactory.getLogger(WordLoaderService::class.java)

    @PostConstruct
    @Transactional
    fun loadWordsFromCSV() {
        // Idempotent: on every boot, ensure each category contains every word from its
        // CSV, inserting only the missing ones. This lets us ship new/harder words and
        // have them sync to an existing database on the next deploy (no wipe needed).
        val categories = categoryRepository.findAll()
        val categoryFiles = mapOf(
            "Animals" to "classpath:data/animals.csv",
            "Food" to "classpath:data/food.csv",
            "Sports" to "classpath:data/sports.csv",
            "Science" to "classpath:data/science.csv",
            "Nature" to "classpath:data/nature.csv",
            "Technology" to "classpath:data/technology.csv"
        )

        categories.forEach { category ->
            val filePath = categoryFiles[category.name] ?: return@forEach
            try {
                val existing = wordRepository.findByCategoryId(category.id)
                    .map { it.word.uppercase() }
                    .toHashSet()

                val resource = resourceLoader.getResource(filePath)
                val parsed = CSVReader(InputStreamReader(resource.inputStream)).use { reader ->
                    reader.readAll()
                        .drop(1) // Skip header
                        .mapNotNull { row ->
                            val w = row.getOrNull(0)?.trim()?.uppercase()
                            if (!w.isNullOrEmpty() && row.size >= 2) {
                                Word(
                                    categoryId = category.id,
                                    word = w,
                                    difficultyLevel = row[1].trim().toIntOrNull() ?: 1
                                )
                            } else null
                        }
                }

                val toAdd = parsed.filter { it.word !in existing }
                if (toAdd.isNotEmpty()) {
                    wordRepository.saveAll(toAdd)
                    logger.info("Added ${toAdd.size} new words for category: ${category.name}")
                }
            } catch (e: Exception) {
                logger.error("Error loading words for category ${category.name}: ${e.message}")
            }
        }
    }
}

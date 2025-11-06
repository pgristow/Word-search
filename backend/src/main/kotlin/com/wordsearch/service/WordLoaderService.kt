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
        // Only load if database is empty
        if (wordRepository.count() > 0) {
            logger.info("Words already loaded in database")
            return
        }

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
            val filePath = categoryFiles[category.name]
            if (filePath != null) {
                try {
                    val resource = resourceLoader.getResource(filePath)
                    val reader = CSVReader(InputStreamReader(resource.inputStream))

                    val words = reader.readAll()
                        .drop(1) // Skip header
                        .mapNotNull { row ->
                            if (row.size >= 2) {
                                Word(
                                    categoryId = category.id,
                                    word = row[0].trim().uppercase(),
                                    difficultyLevel = row[1].trim().toIntOrNull() ?: 1
                                )
                            } else null
                        }

                    wordRepository.saveAll(words)
                    logger.info("Loaded ${words.size} words for category: ${category.name}")
                } catch (e: Exception) {
                    logger.error("Error loading words for category ${category.name}: ${e.message}")
                }
            }
        }
    }
}

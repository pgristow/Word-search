package com.wordsearch.controller

import com.wordsearch.model.Category
import com.wordsearch.repository.CategoryRepository
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/categories")
class CategoryController(
    private val categoryRepository: CategoryRepository
) {

    @GetMapping
    fun getAllCategories(): List<Category> {
        return categoryRepository.findByIsActiveOrderByDisplayOrder()
    }

    @GetMapping("/{id}")
    fun getCategoryById(@PathVariable id: String): Category? {
        return categoryRepository.findById(java.util.UUID.fromString(id)).orElse(null)
    }
}

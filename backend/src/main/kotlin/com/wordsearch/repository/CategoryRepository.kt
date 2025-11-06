package com.wordsearch.repository

import com.wordsearch.model.Category
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface CategoryRepository : JpaRepository<Category, UUID> {
    fun findByIsActiveOrderByDisplayOrder(isActive: Boolean = true): List<Category>
    fun findByName(name: String): Category?
}

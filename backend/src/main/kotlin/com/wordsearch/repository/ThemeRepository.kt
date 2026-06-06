package com.wordsearch.repository

import com.wordsearch.model.Theme
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ThemeRepository : JpaRepository<Theme, UUID> {
    fun findByIsActiveTrue(): List<Theme>
}

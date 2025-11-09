package com.wordsearch.repository

import com.wordsearch.model.UserFoundWord
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface UserFoundWordRepository : JpaRepository<UserFoundWord, UUID> {
    fun findBySessionId(sessionId: UUID): List<UserFoundWord>
    fun findBySessionIdAndWord(sessionId: UUID, word: String): UserFoundWord?
    fun existsBySessionIdAndWord(sessionId: UUID, word: String): Boolean
}

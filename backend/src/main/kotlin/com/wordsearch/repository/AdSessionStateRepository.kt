package com.wordsearch.repository

import com.wordsearch.model.AdSessionState
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface AdSessionStateRepository : JpaRepository<AdSessionState, UUID> {
    fun findByUserId(userId: UUID): AdSessionState?
    fun findTopByUserIdOrderBySessionStartTimeDesc(userId: UUID): AdSessionState?
}

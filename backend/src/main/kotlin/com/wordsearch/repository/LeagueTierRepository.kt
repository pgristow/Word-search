package com.wordsearch.repository

import com.wordsearch.model.LeagueTier
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface LeagueTierRepository : JpaRepository<LeagueTier, Int> {
    fun findByTierOrder(tierOrder: Int): LeagueTier?
}

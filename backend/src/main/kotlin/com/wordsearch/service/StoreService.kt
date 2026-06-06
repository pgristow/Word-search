package com.wordsearch.service

import com.wordsearch.model.Category
import com.wordsearch.model.UnlockType
import com.wordsearch.model.UserOwnedTheme
import com.wordsearch.model.UserProgress
import com.wordsearch.model.UserUnlockedCategory
import com.wordsearch.repository.CategoryRepository
import com.wordsearch.repository.ThemeRepository
import com.wordsearch.repository.UserOwnedThemeRepository
import com.wordsearch.repository.UserUnlockedCategoryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/** A theme as shown in the store, with this user's ownership flag. */
data class StoreThemeDto(
    val id: String,
    val name: String,
    val coinCost: Int,
    val assetKey: String,
    val owned: Boolean
)

/** A coin-lockable category as shown in the store, with this user's unlock flag. */
data class StoreCategoryDto(
    val id: String,
    val name: String,
    val coinUnlockCost: Int,
    val unlocked: Boolean
)

data class StoreResponse(
    val coinBalance: Long,
    val hintCost: Int,
    val themes: List<StoreThemeDto>,
    val lockableCategories: List<StoreCategoryDto>
)

data class PurchaseResult(
    val success: Boolean,
    val coinBalance: Long,
    val message: String
)

@Service
class StoreService(
    private val economyService: EconomyService,
    private val themeRepository: ThemeRepository,
    private val userOwnedThemeRepository: UserOwnedThemeRepository,
    private val categoryRepository: CategoryRepository,
    private val userUnlockedCategoryRepository: UserUnlockedCategoryRepository
) {

    /** The full store view for [userId]: balance, hint price, themes, lockable categories. */
    fun store(userId: UUID): StoreResponse {
        val themes = themeRepository.findByIsActiveTrue().map { theme ->
            StoreThemeDto(
                id = theme.id.toString(),
                name = theme.name,
                coinCost = theme.coinCost,
                assetKey = theme.assetKey,
                owned = userOwnedThemeRepository.existsByUserIdAndThemeId(userId, theme.id)
            )
        }

        val lockableCategories = categoryRepository.findAll()
            .filter { it.coinUnlockCost > 0 }
            .map { category ->
                StoreCategoryDto(
                    id = category.id.toString(),
                    name = category.name,
                    coinUnlockCost = category.coinUnlockCost,
                    unlocked = userUnlockedCategoryRepository
                        .existsByUserIdAndCategoryId(userId, category.id)
                )
            }

        return StoreResponse(
            coinBalance = economyService.balance(userId),
            hintCost = EconomyService.HINT_COST.toInt(),
            themes = themes,
            lockableCategories = lockableCategories
        )
    }

    /**
     * Buys an item for [userId]. [itemType] is "THEME" or "CATEGORY". Theme/category
     * purchases are idempotent: an already-owned/unlocked item succeeds without
     * charging again. Insufficient funds returns a failed (not thrown) result.
     */
    @Transactional
    fun purchase(userId: UUID, itemType: String, itemId: UUID): PurchaseResult =
        when (itemType.uppercase()) {
            "THEME" -> purchaseTheme(userId, itemId)
            "CATEGORY" -> unlockCategory(userId, itemId)
            else -> PurchaseResult(false, economyService.balance(userId), "Unknown item type: $itemType")
        }

    /**
     * Whether [category] is available to [userId]: true if its progression milestone
     * is met OR the user has a coin-unlock row for it. Categories with no requirement
     * (NONE/null) are always available. This is the authoritative server-side resolver
     * combining milestone reconciliation with coin unlocks (Task 9).
     */
    fun isCategoryAvailable(userId: UUID, category: Category, userProgress: UserProgress): Boolean {
        if (milestoneMet(category, userProgress)) return true
        return userUnlockedCategoryRepository.existsByUserIdAndCategoryId(userId, category.id)
    }

    private fun milestoneMet(category: Category, p: UserProgress): Boolean {
        val required = category.unlockRequirementValue ?: 0
        return when (category.unlockRequirementType ?: UnlockType.NONE) {
            UnlockType.NONE -> true
            UnlockType.WORDS -> p.totalWordsFound >= required
            UnlockType.LEVEL -> p.currentLevel >= required
            UnlockType.PUZZLES -> p.casualPuzzlesCompleted >= required
        }
    }

    private fun purchaseTheme(userId: UUID, themeId: UUID): PurchaseResult {
        val theme = themeRepository.findById(themeId).orElse(null)
            ?: return PurchaseResult(false, economyService.balance(userId), "Theme not found")

        if (userOwnedThemeRepository.existsByUserIdAndThemeId(userId, themeId)) {
            return PurchaseResult(true, economyService.balance(userId), "Already owned")
        }

        return try {
            val balance = economyService.spend(userId, theme.coinCost.toLong(), "BUY_THEME", themeId)
            userOwnedThemeRepository.save(UserOwnedTheme(userId = userId, themeId = themeId))
            PurchaseResult(true, balance, "Theme purchased")
        } catch (e: InsufficientCoinsException) {
            PurchaseResult(false, economyService.balance(userId), e.message ?: "Insufficient coins")
        }
    }

    private fun unlockCategory(userId: UUID, categoryId: UUID): PurchaseResult {
        val category = categoryRepository.findById(categoryId).orElse(null)
            ?: return PurchaseResult(false, economyService.balance(userId), "Category not found")

        if (userUnlockedCategoryRepository.existsByUserIdAndCategoryId(userId, categoryId)) {
            return PurchaseResult(true, economyService.balance(userId), "Already unlocked")
        }

        return try {
            val balance = economyService.spend(
                userId, category.coinUnlockCost.toLong(), "UNLOCK_CATEGORY", categoryId
            )
            userUnlockedCategoryRepository.save(
                UserUnlockedCategory(userId = userId, categoryId = categoryId, method = "COINS")
            )
            PurchaseResult(true, balance, "Category unlocked")
        } catch (e: InsufficientCoinsException) {
            PurchaseResult(false, economyService.balance(userId), e.message ?: "Insufficient coins")
        }
    }
}

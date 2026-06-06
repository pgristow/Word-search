package com.wordsearch.service

import com.wordsearch.model.Category
import com.wordsearch.model.Theme
import com.wordsearch.model.UserOwnedTheme
import com.wordsearch.model.UnlockType
import com.wordsearch.model.UserProgress
import com.wordsearch.model.UserUnlockedCategory
import com.wordsearch.repository.CategoryRepository
import com.wordsearch.repository.ThemeRepository
import com.wordsearch.repository.UserOwnedThemeRepository
import com.wordsearch.repository.UserUnlockedCategoryRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Optional
import java.util.UUID

class StoreServiceTest {
    private lateinit var economyService: EconomyService
    private lateinit var themeRepository: ThemeRepository
    private lateinit var userOwnedThemeRepository: UserOwnedThemeRepository
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var userUnlockedCategoryRepository: UserUnlockedCategoryRepository
    private lateinit var service: StoreService

    private val userId = UUID.randomUUID()
    private val themeId = UUID.randomUUID()
    private val categoryId = UUID.randomUUID()

    @BeforeEach
    fun setup() {
        economyService = mockk(relaxed = true)
        themeRepository = mockk(relaxed = true)
        userOwnedThemeRepository = mockk(relaxed = true)
        categoryRepository = mockk(relaxed = true)
        userUnlockedCategoryRepository = mockk(relaxed = true)
        service = StoreService(
            economyService, themeRepository, userOwnedThemeRepository,
            categoryRepository, userUnlockedCategoryRepository
        )
        every { userOwnedThemeRepository.save(any()) } answers { firstArg() }
        every { userUnlockedCategoryRepository.save(any()) } answers { firstArg() }
    }

    @Test fun `theme purchase deducts coins and grants ownership`() {
        every { themeRepository.findById(themeId) } returns
            Optional.of(Theme(id = themeId, name = "Dark", coinCost = 100, assetKey = "dark"))
        every { userOwnedThemeRepository.existsByUserIdAndThemeId(userId, themeId) } returns false
        every { economyService.spend(userId, 100L, "BUY_THEME", themeId) } returns 50L

        val owned = slot<UserOwnedTheme>()
        every { userOwnedThemeRepository.save(capture(owned)) } answers { firstArg() }

        val result = service.purchase(userId, "THEME", themeId)

        assertTrue(result.success)
        assertEquals(50L, result.coinBalance)
        assertEquals(userId, owned.captured.userId)
        assertEquals(themeId, owned.captured.themeId)
        verify { economyService.spend(userId, 100L, "BUY_THEME", themeId) }
    }

    @Test fun `already-owned theme is a no-op success without charging`() {
        every { themeRepository.findById(themeId) } returns
            Optional.of(Theme(id = themeId, name = "Dark", coinCost = 100, assetKey = "dark"))
        every { userOwnedThemeRepository.existsByUserIdAndThemeId(userId, themeId) } returns true
        every { economyService.balance(userId) } returns 999L

        val result = service.purchase(userId, "THEME", themeId)

        assertTrue(result.success)
        assertEquals(999L, result.coinBalance)
        verify(exactly = 0) { economyService.spend(any(), any(), any(), any()) }
        verify(exactly = 0) { userOwnedThemeRepository.save(any()) }
    }

    @Test fun `category unlock inserts a row with COINS method`() {
        every { categoryRepository.findById(categoryId) } returns
            Optional.of(Category(id = categoryId, name = "Space", coinUnlockCost = 200))
        every { userUnlockedCategoryRepository.existsByUserIdAndCategoryId(userId, categoryId) } returns false
        every { economyService.spend(userId, 200L, "UNLOCK_CATEGORY", categoryId) } returns 0L

        val unlocked = slot<UserUnlockedCategory>()
        every { userUnlockedCategoryRepository.save(capture(unlocked)) } answers { firstArg() }

        val result = service.purchase(userId, "CATEGORY", categoryId)

        assertTrue(result.success)
        assertEquals(categoryId, unlocked.captured.categoryId)
        assertEquals("COINS", unlocked.captured.method)
        verify { economyService.spend(userId, 200L, "UNLOCK_CATEGORY", categoryId) }
    }

    @Test fun `insufficient funds returns failure and grants nothing`() {
        every { themeRepository.findById(themeId) } returns
            Optional.of(Theme(id = themeId, name = "Dark", coinCost = 100, assetKey = "dark"))
        every { userOwnedThemeRepository.existsByUserIdAndThemeId(userId, themeId) } returns false
        every { economyService.spend(userId, 100L, "BUY_THEME", themeId) } throws
            InsufficientCoinsException("Insufficient coins: balance 10, required 100")
        every { economyService.balance(userId) } returns 10L

        val result = service.purchase(userId, "THEME", themeId)

        assertFalse(result.success)
        assertEquals(10L, result.coinBalance)
        assertTrue(result.message.contains("Insufficient"))
        verify(exactly = 0) { userOwnedThemeRepository.save(any()) }
    }

    @Test fun `store marks themes owned and categories unlocked`() {
        every { themeRepository.findByIsActiveTrue() } returns listOf(
            Theme(id = themeId, name = "Dark", coinCost = 100, assetKey = "dark")
        )
        every { userOwnedThemeRepository.existsByUserIdAndThemeId(userId, themeId) } returns true
        every { categoryRepository.findAll() } returns listOf(
            Category(id = categoryId, name = "Space", coinUnlockCost = 200),
            Category(name = "Free", coinUnlockCost = 0)
        )
        every { userUnlockedCategoryRepository.existsByUserIdAndCategoryId(userId, categoryId) } returns false
        every { economyService.balance(userId) } returns 500L

        val store = service.store(userId)

        assertEquals(500L, store.coinBalance)
        assertEquals(30, store.hintCost)
        assertEquals(1, store.themes.size)
        assertTrue(store.themes[0].owned)
        // Only coin-lockable categories appear (the free one is filtered out).
        assertEquals(1, store.lockableCategories.size)
        assertEquals("Space", store.lockableCategories[0].name)
        assertFalse(store.lockableCategories[0].unlocked)
    }

    @Test fun `isCategoryAvailable is true when milestone met even without a coin unlock`() {
        val category = Category(
            id = categoryId, name = "Pro",
            unlockRequirementType = UnlockType.WORDS, unlockRequirementValue = 50
        )
        val progress = UserProgress(userId = userId, totalWordsFound = 60)
        every { userUnlockedCategoryRepository.existsByUserIdAndCategoryId(userId, categoryId) } returns false

        assertTrue(service.isCategoryAvailable(userId, category, progress))
    }

    @Test fun `isCategoryAvailable is true via coin unlock when milestone not met`() {
        val category = Category(
            id = categoryId, name = "Pro",
            unlockRequirementType = UnlockType.LEVEL, unlockRequirementValue = 20
        )
        val progress = UserProgress(userId = userId, currentLevel = 3)
        every { userUnlockedCategoryRepository.existsByUserIdAndCategoryId(userId, categoryId) } returns true

        assertTrue(service.isCategoryAvailable(userId, category, progress))
    }

    @Test fun `isCategoryAvailable is false when neither milestone nor coin unlock satisfied`() {
        val category = Category(
            id = categoryId, name = "Pro",
            unlockRequirementType = UnlockType.PUZZLES, unlockRequirementValue = 10
        )
        val progress = UserProgress(userId = userId, casualPuzzlesCompleted = 2)
        every { userUnlockedCategoryRepository.existsByUserIdAndCategoryId(userId, categoryId) } returns false

        assertFalse(service.isCategoryAvailable(userId, category, progress))
    }
}

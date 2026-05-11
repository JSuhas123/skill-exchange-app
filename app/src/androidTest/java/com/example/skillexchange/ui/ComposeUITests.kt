package com.example.skillexchange.ui

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.skillexchange.ui.components.PremiumButton
import com.example.skillexchange.ui.theme.SkillExchangeTheme
import org.junit.Rule
import org.junit.Test

/**
 * Compose UI tests for button components
 */
@ExperimentalMaterial3Api
class ButtonComponentsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testPremiumButtonRendering() {
        var clicked = false

        composeTestRule.setContent {
            SkillExchangeTheme {
                PremiumButton(
                    text = "Test Button",
                    onClick = { clicked = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Test Button")
            .assertIsDisplayed()
            .performClick()

        assert(clicked)
    }

    @Test
    fun testPremiumButtonDisabledState() {
        composeTestRule.setContent {
            SkillExchangeTheme {
                PremiumButton(
                    text = "Disabled",
                    onClick = {},
                    enabled = false
                )
            }
        }

        composeTestRule.onNodeWithText("Disabled")
            .assertIsNotEnabled()
    }

    @Test
    fun testPremiumButtonLoadingState() {
        composeTestRule.setContent {
            SkillExchangeTheme {
                PremiumButton(
                    text = "Save",
                    onClick = {},
                    isLoading = true
                )
            }
        }

        // Should show loading indicator and disable button
        composeTestRule.onNodeWithText("Save")
            .assertIsNotEnabled()
    }
}

/**
 * Compose UI tests for card components
 */
class CardComponentsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testPostCardRendering() {
        composeTestRule.setContent {
            SkillExchangeTheme {
                PostCard(
                    post = Post(
                        id = "1",
                        userId = "user1",
                        skillRequired = "Photography",
                        skillOffered = "Web Development",
                        description = "Test post",
                        timestamp = 1234567890
                    ),
                    onChatClick = {},
                    onSwapClick = {},
                    userName = "John Doe"
                )
            }
        }

        composeTestRule.onNodeWithText("Photography")
            .assertIsDisplayed()

        composeTestRule.onNodeWithText("Web Development")
            .assertIsDisplayed()
    }

    @Test
    fun testProfileCardRendering() {
        val testUser = User(
            id = "user1",
            name = "Jane Doe",
            skillPoints = 100,
            trustScore = 95,
            skillsOffered = listOf("Photography"),
            skillsNeeded = listOf("Web Development")
        )

        composeTestRule.setContent {
            SkillExchangeTheme {
                ProfileCard(user = testUser)
            }
        }

        composeTestRule.onNodeWithText("Jane Doe")
            .assertIsDisplayed()
    }
}

/**
 * Compose UI tests for state components
 */
class StateComponentsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testLoadingStateDisplay() {
        composeTestRule.setContent {
            SkillExchangeTheme {
                FullScreenLoading(message = "Loading posts...")
            }
        }

        composeTestRule.onNodeWithText("Loading posts...")
            .assertIsDisplayed()
    }

    @Test
    fun testErrorStateWithRetry() {
        var retryClicked = false

        composeTestRule.setContent {
            SkillExchangeTheme {
                ErrorView(
                    message = "Network error",
                    onRetry = { retryClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Network error")
            .assertIsDisplayed()

        composeTestRule.onNodeWithText("Try Again")
            .performClick()

        assert(retryClicked)
    }

    @Test
    fun testEmptyStateDisplay() {
        composeTestRule.setContent {
            SkillExchangeTheme {
                EmptyView(
                    title = "No Posts",
                    message = "Create your first post"
                )
            }
        }

        composeTestRule.onNodeWithText("No Posts")
            .assertIsDisplayed()

        composeTestRule.onNodeWithText("Create your first post")
            .assertIsDisplayed()
    }
}

/**
 * Compose UI tests for input components
 */
class InputComponentsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testTextFieldInput() {
        composeTestRule.setContent {
            SkillExchangeTheme {
                PremiumTextField(
                    value = "",
                    onValueChange = {},
                    label = "Email"
                )
            }
        }

        composeTestRule.onNodeWithText("Email")
            .assertIsDisplayed()
    }

    @Test
    fun testTextFieldValidationError() {
        composeTestRule.setContent {
            SkillExchangeTheme {
                PremiumTextField(
                    value = "",
                    onValueChange = {},
                    label = "Email",
                    isError = true,
                    errorText = "Invalid email"
                )
            }
        }

        composeTestRule.onNodeWithText("Invalid email")
            .assertIsDisplayed()
    }

    @Test
    fun testSkillChipRendering() {
        composeTestRule.setContent {
            SkillExchangeTheme {
                SkillChip(text = "Photography")
            }
        }

        composeTestRule.onNodeWithText("Photography")
            .assertIsDisplayed()
    }
}

/**
 * Compose UI tests for navigation components
 */
class NavigationComponentsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testTopAppBarRendering() {
        composeTestRule.setContent {
            SkillExchangeTheme {
                PremiumTopAppBar(title = "Skill Board")
            }
        }

        composeTestRule.onNodeWithText("Skill Board")
            .assertIsDisplayed()
    }

    @Test
    fun testNavigationBarItemSelection() {
        var selectedIndex = -1

        composeTestRule.setContent {
            SkillExchangeTheme {
                PremiumNavigationBar(
                    selectedItem = 0,
                    onItemSelected = { selectedIndex = it },
                    items = listOf(
                        NavigationItem(Icons.Default.Home, "Home", "home"),
                        NavigationItem(Icons.Default.Search, "Search", "search")
                    )
                )
            }
        }

        composeTestRule.onNodeWithText("Home")
            .performClick()
    }
}

/**
 * Integration tests for screen flows
 */
class ScreenFlowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testSkillBoardScreenFlow() {
        composeTestRule.setContent {
            SkillExchangeTheme {
                // Test full screen rendering
                SkillBoardScreenRefactored(
                    onChatClick = {}
                )
            }
        }

        // Verify key elements are present
        composeTestRule.onNodeWithText("Skill Board")
            .assertIsDisplayed()
    }

    @Test
    fun testProfileScreenViewMode() {
        val testUser = User(
            id = "user1",
            name = "Test User",
            skillPoints = 100,
            trustScore = 95,
            skillsOffered = listOf("Photography"),
            skillsNeeded = listOf("Web Development")
        )

        composeTestRule.setContent {
            SkillExchangeTheme {
                ProfileScreenRefactored(onSignOut = {})
            }
        }

        // Verify profile elements
        composeTestRule.onNodeWithText("Profile")
            .assertIsDisplayed()
    }
}

package com.example.skillexchange.utils

import com.example.skillexchange.data.model.User
import com.example.skillexchange.utils.Resource
import com.example.skillexchange.utils.InputValidator
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for InputValidator utility
 */
class InputValidatorTest {

    @Test
    fun testValidEmail() {
        assertTrue(InputValidator.isValidEmail("user@example.com"))
        assertTrue(InputValidator.isValidEmail("john.doe@company.co.uk"))
    }

    @Test
    fun testInvalidEmail() {
        assertFalse(InputValidator.isValidEmail("invalid.email"))
        assertFalse(InputValidator.isValidEmail("@example.com"))
        assertFalse(InputValidator.isValidEmail("user@"))
    }

    @Test
    fun testValidName() {
        assertTrue(InputValidator.isValidName("John Doe"))
        assertTrue(InputValidator.isValidName("AB"))
        assertTrue(InputValidator.isValidName("A".repeat(100)))
    }

    @Test
    fun testInvalidName() {
        assertFalse(InputValidator.isValidName("A")) // Too short
        assertFalse(InputValidator.isValidName("A".repeat(101))) // Too long
        assertFalse(InputValidator.isValidName(""))
    }

    @Test
    fun testValidSkill() {
        assertTrue(InputValidator.isValidSkill("Photography"))
        assertTrue(InputValidator.isValidSkill("Web Development"))
        assertTrue(InputValidator.isValidSkill("AI"))
    }

    @Test
    fun testInvalidSkill() {
        assertFalse(InputValidator.isValidSkill("X")) // Too short
        assertFalse(InputValidator.isValidSkill("A".repeat(101))) // Too long
    }

    @Test
    fun testValidDescription() {
        assertTrue(InputValidator.isValidDescription("This is a valid description with enough characters"))
        assertTrue(InputValidator.isValidDescription("A".repeat(1000))) // Max length
    }

    @Test
    fun testInvalidDescription() {
        assertFalse(InputValidator.isValidDescription("ABC")) // Too short
        assertFalse(InputValidator.isValidDescription("A".repeat(1001))) // Too long
    }

    @Test
    fun testValidHours() {
        assertTrue(InputValidator.isValidHours(1))
        assertTrue(InputValidator.isValidHours(168)) // Max hours in week
        assertTrue(InputValidator.isValidHours(50))
    }

    @Test
    fun testInvalidHours() {
        assertFalse(InputValidator.isValidHours(0)) // Minimum is 1
        assertFalse(InputValidator.isValidHours(169)) // Maximum is 168
        assertFalse(InputValidator.isValidHours(-5))
    }

    @Test
    fun testParseSkillList() {
        val skills = InputValidator.parseSkillList("Photography, Web Dev, Design")
        assertEquals(3, skills.size)
        assertEquals("Photography", skills[0])
        assertEquals("Web Dev", skills[1])
    }

    @Test
    fun testValidSkillList() {
        assertTrue(InputValidator.isValidSkillList("Photography, Web Development"))
        assertTrue(InputValidator.isValidSkillList("AI"))
    }

    @Test
    fun testInvalidSkillList() {
        assertFalse(InputValidator.isValidSkillList("")) // Empty
        assertFalse(InputValidator.isValidSkillList("X")) // Skill too short
    }
}

/**
 * Unit tests for ErrorHandler utility
 */
class ErrorHandlerTest {

    @Test
    fun testIsNetworkError() {
        val networkError = Exception("Unable to resolve host")
        assertTrue(ErrorHandler.isNetworkError(networkError))

        val timeoutError = Exception("Socket timeout")
        assertTrue(ErrorHandler.isNetworkError(timeoutError))

        val normalError = Exception("Something else")
        assertFalse(ErrorHandler.isNetworkError(normalError))
    }

    @Test
    fun testIsRetryableError() {
        val networkError = Exception("Network error")
        assertTrue(ErrorHandler.isRetryableError(networkError))

        val fataleError = Exception("400 Bad Request")
        assertFalse(ErrorHandler.isRetryableError(fataleError))
    }

    @Test
    fun testGetErrorMessage() {
        val networkError = Exception("Connection refused")
        val message = ErrorHandler.getErrorMessage(networkError)
        assertTrue(message.isNotEmpty())
        assertTrue(message.contains("Network") || message.contains("connection"))
    }
}

/**
 * Unit tests for Resource wrapper
 */
class ResourceTest {

    @Test
    fun testSuccessResource() {
        val resource = Resource.Success("Test Data")
        assertTrue(resource.isSuccess())
        assertFalse(resource.isError())
        assertFalse(resource.isLoading())
        assertEquals("Test Data", resource.getOrNull())
    }

    @Test
    fun testErrorResource() {
        val resource = Resource.Error<String>("Error message")
        assertFalse(resource.isSuccess())
        assertTrue(resource.isError())
        assertFalse(resource.isLoading())
        assertNull(resource.getOrNull())
    }

    @Test
    fun testLoadingResource() {
        val resource = Resource.Loading<String>()
        assertFalse(resource.isSuccess())
        assertFalse(resource.isError())
        assertTrue(resource.isLoading())
    }

    @Test
    fun testResourceMap() {
        val resource = Resource.Success("5")
        val mapped = resource.map { it.toInt() }
        assertTrue(mapped is Resource.Success)
        assertEquals(5, (mapped as Resource.Success).data)
    }

    @Test
    fun testResourceOnSuccess() {
        var wasCalled = false
        Resource.Success("data").onSuccess { wasCalled = true }
        assertTrue(wasCalled)

        wasCalled = false
        Resource.Error<String>("error").onSuccess { wasCalled = true }
        assertFalse(wasCalled)
    }

    @Test
    fun testResourceOnError() {
        var wasCalled = false
        Resource.Error<String>("error").onError { wasCalled = true }
        assertTrue(wasCalled)

        wasCalled = false
        Resource.Success("data").onError { wasCalled = true }
        assertFalse(wasCalled)
    }
}

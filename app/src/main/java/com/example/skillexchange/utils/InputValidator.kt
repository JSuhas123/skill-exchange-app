package com.example.skillexchange.utils

object InputValidator {
    fun isValidEmail(email: String): Boolean {
        return email.matches(Regex("^[A-Za-z0-9+_.-]+@(.+)$"))
    }
    
    fun isValidName(name: String): Boolean {
        return name.isNotBlank() && name.length >= 2 && name.length <= 100
    }
    
    fun isValidSkill(skill: String): Boolean {
        return skill.isNotBlank() && skill.length >= 2 && skill.length <= 100
    }
    
    fun isValidDescription(description: String): Boolean {
        return description.isNotBlank() && description.length >= 5 && description.length <= 1000
    }
    
    fun isValidHours(hours: Int): Boolean {
        return hours in 1..168
    }

    /** Accepts E.164-like input: optional '+' prefix, first digit 1-9, total 10-15 digits. */
    fun isValidPhoneNumber(phoneNumber: String): Boolean {
        return phoneNumber.matches(Regex("^\\+?[1-9]\\d{9,14}$"))
    }
    
    fun parseSkillList(input: String): List<String> {
        return input.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() && isValidSkill(it) }
    }
    
    fun isValidSkillList(skills: List<String>): Boolean {
        return skills.isNotEmpty() && skills.all { isValidSkill(it) }
    }
}

package com.example.skillexchange.viewmodel

import androidx.lifecycle.ViewModel
import com.example.skillexchange.data.repository.SkillRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: SkillRepository
) : ViewModel() {
    // Add ViewModel logic here
}

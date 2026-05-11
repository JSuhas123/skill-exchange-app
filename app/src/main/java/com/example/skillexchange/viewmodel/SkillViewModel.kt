package com.example.skillexchange.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skillexchange.data.model.Skill
import com.example.skillexchange.data.repository.SkillRepository
import com.example.skillexchange.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SkillViewModel @Inject constructor(
    private val repository: SkillRepository
) : ViewModel() {

    private val _skills = MutableStateFlow<Resource<List<Skill>>>(Resource.Loading())
    val skills: StateFlow<Resource<List<Skill>>> = _skills

    init {
        fetchSkills()
    }

    fun fetchSkills() {
        viewModelScope.launch {
            repository.getSkills().collect { resource ->
                _skills.value = resource
            }
        }
    }
}

package com.example.skillexchange.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skillexchange.data.model.Skill
import com.example.skillexchange.data.repository.SkillRepository
import com.example.skillexchange.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: SkillRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val searchResults: StateFlow<Resource<List<Skill>>> = _searchQuery
        .debounce(300L)
        .flatMapLatest { query ->
            if (query.isBlank()) {
                flowOf(Resource.Success(emptyList()))
            } else {
                repository.getSkills().map { resource ->
                    if (resource is Resource.Success) {
                        val filtered = resource.data?.filter { 
                            it.name.contains(query, ignoreCase = true) || 
                            it.description.contains(query, ignoreCase = true) 
                        } ?: emptyList()
                        Resource.Success(filtered)
                    } else {
                        resource
                    }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, Resource.Success(emptyList()))

    fun searchSkills(query: String) {
        _searchQuery.value = query
    }
}

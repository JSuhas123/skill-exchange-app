package com.example.skillexchange.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skillexchange.data.model.Post
import com.example.skillexchange.data.repository.PostRepository
import com.example.skillexchange.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class SkillBoardViewModel @Inject constructor(
    private val postRepository: PostRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _posts = postRepository.getPosts()
        .stateIn(viewModelScope, SharingStarted.Lazily, Resource.Loading())

    val filteredPosts: StateFlow<Resource<List<Post>>> = combine(_posts, _searchQuery) { resource, query ->
        if (resource is Resource.Success) {
            val filteredList = resource.data?.filter { post ->
                post.skillRequired.contains(query, ignoreCase = true) ||
                post.skillOffered.contains(query, ignoreCase = true) ||
                post.description.contains(query, ignoreCase = true)
            } ?: emptyList()
            Resource.Success(filteredList)
        } else {
            resource
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, Resource.Loading())

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }
}

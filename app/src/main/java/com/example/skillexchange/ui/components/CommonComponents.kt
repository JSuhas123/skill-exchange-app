package com.example.skillexchange.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.skillexchange.data.model.Post
import com.example.skillexchange.data.model.Skill

/**
 * Backward compatibility: PremiumPostCard mapped to new PostCard
 */
@Composable
fun PremiumPostCard(
    post: Post,
    onChatClick: () -> Unit,
    onSwapClick: () -> Unit,
    modifier: Modifier = Modifier,
    isOwnPost: Boolean = false,
    isSwapLoading: Boolean = false,
    userName: String = "User"
) {
    PostCard(
        post = post,
        onChatClick = onChatClick,
        onSwapClick = onSwapClick,
        modifier = modifier,
        isOwnPost = isOwnPost,
        isSwapLoading = isSwapLoading,
        userName = userName
    )
}

/**
 * Backward compatibility: PremiumSkillCard mapped to new SkillCard
 */
@Composable
fun PremiumSkillCard(
    skill: Skill,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier,
    actionText: String = "Request"
) {
    SkillCard(
        skill = skill,
        onActionClick = onActionClick,
        modifier = modifier,
        actionText = actionText
    )
}

@Composable
fun LoadingScreen() {
    FullScreenLoading()
}

/**
 * Search bar component with Material 3 styling
 */
@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search skills, users...",
    onClear: (() -> Unit)? = null
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        placeholder = { Text(placeholder) },
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
        leadingIcon = {
            Icon(
                imageVector = androidx.compose.material.icons.filled.Search,
                contentDescription = "Search",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingIcon = if (query.isNotEmpty() && onClear != null) {
            {
                IconButton(onClick = onClear) {
                    Icon(
                        imageVector = androidx.compose.material.icons.filled.Clear,
                        contentDescription = "Clear",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else null,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
        ),
        textStyle = MaterialTheme.typography.bodyMedium
    )
}

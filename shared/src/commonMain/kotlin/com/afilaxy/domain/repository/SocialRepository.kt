package com.afilaxy.domain.repository

import com.afilaxy.domain.model.*
import kotlinx.coroutines.flow.Flow

interface SocialRepository {
    suspend fun getFeedPosts(type: PostType? = null): Flow<List<SocialPost>>
    suspend fun createPost(post: SocialPost): Result<Unit>
    suspend fun likePost(postId: String, userId: String): Result<Unit>
    suspend fun unlikePost(postId: String, userId: String): Result<Unit>
    suspend fun addComment(postId: String, userId: String, comment: String): Result<Unit>
    suspend fun getCommunityStats(): Flow<CommunityStats>
    suspend fun getQuickActions(): List<QuickAction>
    suspend fun getAirQuality(location: String): Result<AirQuality>
}
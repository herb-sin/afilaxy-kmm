package com.afilaxy.data.repository

import com.afilaxy.domain.model.*
import com.afilaxy.domain.repository.SocialRepository
import com.afilaxy.util.TimeUtils
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class SocialRepositoryImpl(
    private val firestore: FirebaseFirestore
) : SocialRepository {

    override suspend fun getFeedPosts(type: PostType?): Flow<List<SocialPost>> = flow {
        try {
            val query = if (type != null) {
                firestore.collection("social_posts")
                    .where { "type" equalTo type }
                    .orderBy("timestamp", dev.gitlive.firebase.firestore.Direction.DESCENDING)
            } else {
                firestore.collection("social_posts")
                    .orderBy("timestamp", dev.gitlive.firebase.firestore.Direction.DESCENDING)
            }
            
            query.snapshots.collect { querySnapshot ->
                val posts = querySnapshot.documents.map { doc ->
                    doc.data<SocialPost>()
                }
                emit(posts)
            }
        } catch (e: Exception) {
            emit(listOf(
                SocialPost(
                    id = "1",
                    userId = "user1",
                    userName = "Mariana Silva",
                    content = "Hoje o ar está muito seco aqui em Curitiba. Alguém tem alguma dica de como manter a umidade em casa sem gastar muito com umidificadores caros? Minha asma está atacando um pouco.",
                    timestamp = TimeUtils.currentTimeMillis() - 7200000,
                    type = PostType.SUPPORT,
                    likes = 24,
                    comments = 12
                ),
                SocialPost(
                    id = "2",
                    userId = "professional1",
                    userName = "Dr. Ricardo Lopes",
                    content = "Atenção pessoal, com a frente fria chegando, lembrem-se de manter os cachecóis à mão. O ar frio direto nos pulmões pode ser um gatilho forte.",
                    timestamp = TimeUtils.currentTimeMillis() - 14400000,
                    type = PostType.MEDICAL,
                    likes = 156,
                    comments = 8,
                    imageUrl = "medical_lungs_image.jpg"
                )
            ))
        }
    }

    override suspend fun createPost(post: SocialPost): Result<Unit> {
        return try {
            firestore.collection("social_posts").document(post.id).set(post)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun likePost(postId: String, userId: String): Result<Unit> {
        return try {
            val postRef = firestore.collection("social_posts").document(postId)
            val likeRef = firestore.collection("post_likes").document("${postId}_${userId}")
            
            likeRef.set(mapOf(
                "postId" to postId,
                "userId" to userId,
                "timestamp" to TimeUtils.currentTimeMillis()
            ))
            
            val post = postRef.get().data<SocialPost>()
            postRef.set(post.copy(likes = post.likes + 1, isLiked = true))
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun unlikePost(postId: String, userId: String): Result<Unit> {
        return try {
            val postRef = firestore.collection("social_posts").document(postId)
            val likeRef = firestore.collection("post_likes").document("${postId}_${userId}")
            
            likeRef.delete()
            
            val post = postRef.get().data<SocialPost>()
            postRef.set(post.copy(likes = maxOf(0, post.likes - 1), isLiked = false))
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addComment(postId: String, userId: String, comment: String): Result<Unit> {
        return try {
            val commentData = mapOf(
                "postId" to postId,
                "userId" to userId,
                "comment" to comment,
                "timestamp" to TimeUtils.currentTimeMillis()
            )
            
            firestore.collection("post_comments").add(commentData)
            
            val postRef = firestore.collection("social_posts").document(postId)
            val post = postRef.get().data<SocialPost>()
            postRef.set(post.copy(comments = post.comments + 1))
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCommunityStats(): Flow<CommunityStats> = flow {
        try {
            emit(CommunityStats(
                onlineCount = 1200,
                activeUsers = emptyList(),
                totalMembers = 15000
            ))
        } catch (e: Exception) {
            emit(CommunityStats(
                onlineCount = 0,
                activeUsers = emptyList(),
                totalMembers = 0
            ))
        }
    }

    override suspend fun getQuickActions(): List<QuickAction> {
        return listOf(
            QuickAction(
                id = "1",
                title = "Encontrar Farmácias",
                icon = "pharmacy",
                action = ActionType.FIND_PHARMACIES
            ),
            QuickAction(
                id = "2",
                title = "Contatos de Emergência",
                icon = "phone",
                action = ActionType.EMERGENCY_CONTACTS
            ),
            QuickAction(
                id = "3",
                title = "Protocolo de Crise",
                icon = "document",
                action = ActionType.CRISIS_PROTOCOL
            )
        )
    }

    override suspend fun getAirQuality(location: String): Result<AirQuality> {
        return try {
            val airQuality = AirQuality(
                index = 42,
                status = "Good",
                pollenCount = "Pollen count is low today in Bombinhas. Safe to go out.",
                location = "Bombinhas",
                recommendation = "Pollen count is low today in Bombinhas. Safe to go out."
            )
            Result.success(airQuality)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
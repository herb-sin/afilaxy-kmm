package com.afilaxy.domain.repository

import com.afilaxy.domain.model.ContentCategory
import com.afilaxy.domain.model.EducationalContent

interface EducationalContentRepository {
    suspend fun getAll(): List<EducationalContent>
    suspend fun getById(id: String): EducationalContent?
    suspend fun getByCategory(category: ContentCategory): List<EducationalContent>
}

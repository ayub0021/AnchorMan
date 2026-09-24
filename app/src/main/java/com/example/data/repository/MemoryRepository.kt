package com.example.data.repository

import com.example.data.local.MemoryDao
import com.example.data.model.Memory
import kotlinx.coroutines.flow.Flow

class MemoryRepository(private val memoryDao: MemoryDao) {
    val allMemories: Flow<List<Memory>> = memoryDao.getAllMemories()

    suspend fun insert(memory: Memory): Long {
        return memoryDao.insertMemory(memory)
    }

    suspend fun update(memory: Memory) {
        memoryDao.updateMemory(memory)
    }

    suspend fun delete(memory: Memory) {
        memoryDao.deleteMemory(memory)
    }

    suspend fun deleteById(id: Long) {
        memoryDao.deleteMemoryById(id)
    }

    suspend fun getCount(): Int {
        return memoryDao.getCount()
    }
}

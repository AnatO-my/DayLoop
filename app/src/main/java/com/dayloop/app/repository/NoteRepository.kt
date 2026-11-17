package com.dayloop.app.repository

import com.dayloop.app.core.database.NoteDao
import com.dayloop.app.core.database.NoteEntity
import com.dayloop.app.core.model.Note
import com.dayloop.app.settings.Note as LegacySettingsNote
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface NoteRepository {
    fun observeAllNotes(): Flow<List<Note>>
    fun searchNotes(query: String): Flow<List<Note>>
    suspend fun getNoteById(id: String): Note?
    suspend fun saveNote(note: Note)
    suspend fun deleteNote(id: String)
    suspend fun migrateLegacyNotes(legacyNotes: List<LegacySettingsNote>)
}

class RoomNoteRepository(
    private val noteDao: NoteDao
) : NoteRepository {

    override fun observeAllNotes(): Flow<List<Note>> {
        return noteDao.observeAllNotes().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun searchNotes(query: String): Flow<List<Note>> {
        return if (query.isBlank()) {
            observeAllNotes()
        } else {
            noteDao.searchNotes(query.trim()).map { entities ->
                entities.map { it.toDomainModel() }
            }
        }
    }

    override suspend fun getNoteById(id: String): Note? {
        return noteDao.getNoteById(id)?.toDomainModel()
    }

    override suspend fun saveNote(note: Note) {
        noteDao.insertNote(note.toEntity())
    }

    override suspend fun deleteNote(id: String) {
        noteDao.deleteNoteById(id)
    }

    override suspend fun migrateLegacyNotes(legacyNotes: List<LegacySettingsNote>) {
        legacyNotes.forEach { legacyNote ->
            val existing = noteDao.getNoteById(legacyNote.id)
            if (existing == null) {
                noteDao.insertNote(
                    NoteEntity(
                        id = legacyNote.id,
                        title = "Migrated Note",
                        content = legacyNote.content,
                        createdAt = legacyNote.createdAt,
                        updatedAt = legacyNote.createdAt,
                        tags = "migrated"
                    )
                )
            }
        }
    }

    private fun NoteEntity.toDomainModel(): Note {
        return Note(
            id = id,
            title = title,
            content = content,
            isPinned = isPinned,
            isArchived = isArchived,
            linkedTaskId = linkedTaskId,
            linkedProjectId = linkedProjectId,
            createdAt = createdAt,
            updatedAt = updatedAt,
            tags = tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        )
    }

    private fun Note.toEntity(): NoteEntity {
        return NoteEntity(
            id = id,
            title = title,
            content = content,
            isPinned = isPinned,
            isArchived = isArchived,
            linkedTaskId = linkedTaskId,
            linkedProjectId = linkedProjectId,
            createdAt = createdAt,
            updatedAt = updatedAt,
            tags = tags.joinToString(",")
        )
    }
}

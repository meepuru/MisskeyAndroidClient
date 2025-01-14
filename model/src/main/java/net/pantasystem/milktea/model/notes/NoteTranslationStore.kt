package net.pantasystem.milktea.model.notes

import kotlinx.coroutines.flow.Flow
import net.pantasystem.milktea.common.State
import net.pantasystem.milktea.common.StateContent

data class NoteTranslationsState(
    val noteIdWithTranslation: Map<Note.Id, Translation>,
    val loadings: Set<Note.Id>,
    val loadFails: Map<Note.Id, Throwable>
) {
    fun isLoading(noteId: Note.Id) : Boolean{
        return loadings.contains(noteId)
    }

    fun translation(noteId: Note.Id) : Translation? {
        return noteIdWithTranslation[noteId]
    }

    fun loading(noteId: Note.Id) : NoteTranslationsState {
        return this.copy(
            loadings = this.loadings.toMutableSet().also {
                it.add(noteId)
            }
        )
    }

    fun complete(noteId: Note.Id, translate: Translation?, throwable: Throwable?) : NoteTranslationsState {
        return this.copy(
            loadings = this.loadings.toMutableSet().also {
                it.remove(noteId)
            },
            noteIdWithTranslation = this.noteIdWithTranslation.toMutableMap().also {
                if(translate != null){
                    it[noteId] = translate
                }
            },
            loadFails = this.loadFails.toMutableMap().also {
                if(throwable == null) {
                    it.remove(noteId)
                }else{
                    it[noteId] = throwable
                }
            }
        )
    }


    fun state(noteId: Note.Id): State<Translation> {
        val translation = translation(noteId)
        val isLoading = isLoading(noteId)
        val error = loadFails[noteId]
        val content = if (translation == null) {
            StateContent.NotExist()
        } else {
            StateContent.Exist(translation)
        }
        return when {
            isLoading -> {
                State.Loading(content)
            }
            error != null -> {
                State.Error(content, error)
            }
            else -> {
                State.Fixed(content)
            }
        }

    }
}

interface NoteTranslationStore {
    fun state(id: Note.Id): Flow<State<Translation>>
    suspend fun translate(noteId: Note.Id)
}

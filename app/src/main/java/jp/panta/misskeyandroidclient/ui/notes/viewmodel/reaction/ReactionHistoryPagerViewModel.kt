package jp.panta.misskeyandroidclient.ui.notes.viewmodel.reaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import net.pantasystem.milktea.data.model.notes.NoteCaptureAPIAdapter
import jp.panta.misskeyandroidclient.viewmodel.MiCore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
class ReactionHistoryPagerViewModel(
    val noteId: net.pantasystem.milktea.model.notes.Note.Id,
    val noteRepository: net.pantasystem.milktea.model.notes.NoteRepository,
    val adapter: NoteCaptureAPIAdapter,
    val logger: net.pantasystem.milktea.common.Logger?
) : ViewModel() {

    @Suppress("UNCHECKED_CAST")
    class Factory(val noteId: net.pantasystem.milktea.model.notes.Note.Id, val miCore: MiCore) : ViewModelProvider.Factory{
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ReactionHistoryPagerViewModel(noteId = noteId,
                noteRepository = miCore.getNoteRepository(),
                adapter = miCore.getNoteCaptureAdapter(),
                logger = miCore.loggerFactory.create("ReactionHistoryPagerVM")) as T
        }
    }

    private val mNote = MutableStateFlow<net.pantasystem.milktea.model.notes.Note?>(null)
    val note: StateFlow<net.pantasystem.milktea.model.notes.Note?> = mNote
    val types: Flow<List<net.pantasystem.milktea.model.notes.reaction.ReactionHistoryRequest>> = note.mapNotNull { note ->
        note?.id?.let {  note.id to note.reactionCounts }
    }.map { idAndList ->
        idAndList.second.map { count ->
            count.reaction
        }.map {
            net.pantasystem.milktea.model.notes.reaction.ReactionHistoryRequest(idAndList.first, it)
        }
    }.shareIn(viewModelScope, SharingStarted.Eagerly)

    init {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                noteRepository.find(noteId)
            }.onSuccess {
                mNote.value = it
            }.onFailure {
                logger?.debug("ノート取得エラー noteId: $noteId", e = it)
            }
        }

        adapter.capture(noteId).mapNotNull {
            (it as? net.pantasystem.milktea.model.notes.NoteDataSource.Event.Updated)?.note?: (it as? net.pantasystem.milktea.model.notes.NoteDataSource.Event.Created)?.note
        }.onEach {
            mNote.value = it
        }
    }

}
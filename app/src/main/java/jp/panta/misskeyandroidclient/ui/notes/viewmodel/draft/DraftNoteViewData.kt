package jp.panta.misskeyandroidclient.ui.notes.viewmodel.draft

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import net.pantasystem.milktea.model.notes.draft.DraftNote

class DraftNoteViewData(draftNote: net.pantasystem.milktea.model.notes.draft.DraftNote){

    val note = MutableLiveData(draftNote)

    val isFoldingContent = MediatorLiveData<Boolean>().apply{
        addSource(note){
            value = it.cw != null
        }
    }

    fun toggleFoldingContent(){
        val now = isFoldingContent.value?: false
        val exNote = note.value
        isFoldingContent.value = exNote?.cw != null && !now
    }
}
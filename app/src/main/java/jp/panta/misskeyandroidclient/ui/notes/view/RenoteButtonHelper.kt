package jp.panta.misskeyandroidclient.ui.notes.view

import android.widget.ImageButton
import androidx.databinding.BindingAdapter
import jp.panta.misskeyandroidclient.ui.notes.viewmodel.NotesViewModel
import jp.panta.misskeyandroidclient.ui.notes.viewmodel.PlaneNoteViewData

object RenoteButtonHelper {

    @JvmStatic
    @BindingAdapter("notesViewModelForClickRenote", "clickTargetNote")
    fun ImageButton.renoteButtonClickHelper(notesViewModelForClickRenote: NotesViewModel?, clickTargetNote: PlaneNoteViewData?) {
        if(notesViewModelForClickRenote == null || clickTargetNote == null) {
            return
        }
        this.setOnClickListener {
            notesViewModelForClickRenote.setTargetToReNote(clickTargetNote)
        }
        this.setOnLongClickListener {
            notesViewModelForClickRenote.showRenotes(clickTargetNote.toShowNote.note.id)
            false
        }
    }
}

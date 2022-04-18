package jp.panta.misskeyandroidclient.ui.notes.viewmodel

import net.pantasystem.milktea.data.model.notes.NoteCaptureAPIAdapter
import java.lang.IllegalArgumentException

class HasReplyToNoteViewData(
    noteRelation: net.pantasystem.milktea.model.notes.NoteRelation,
    account: net.pantasystem.milktea.model.account.Account,
    noteCaptureAPIAdapter: NoteCaptureAPIAdapter,
    noteTranslationStore: net.pantasystem.milktea.model.notes.NoteTranslationStore,
)  : PlaneNoteViewData(noteRelation, account, noteCaptureAPIAdapter, noteTranslationStore){
    val reply = noteRelation.reply

    /*val replyToAvatarUrl = reply?.user?.avatarUrl
    val replyToName = reply?.user?.name
    val replyToUserName = reply?.user?.userName
    val replyToText = reply?.text

    val replyToCw = reply?.cw
    //true　折り畳み
    val replyToContentFolding = MutableLiveData<Boolean>( replyToCw != null )
    val replyToContentFoldingStatusMessage = Transformations.map(replyToContentFolding){
        if(it) "もっと見る: ${subNoteText?.length}" else "閉じる"
    }
    fun changeReplyToContentFolding(){

    }*/
    val replyTo = if(reply == null){
        throw IllegalArgumentException("replyがnullですPlaneNoteViewDataを利用してください")
    }else{
        PlaneNoteViewData(reply, account, noteCaptureAPIAdapter, noteTranslationStore)
    }




}
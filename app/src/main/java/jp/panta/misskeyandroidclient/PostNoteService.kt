package jp.panta.misskeyandroidclient

import android.app.IntentService
import android.content.Intent
import android.util.Log
import com.google.gson.GsonBuilder
import jp.panta.misskeyandroidclient.model.drive.OkHttpDriveFileUploader
import jp.panta.misskeyandroidclient.viewmodel.notes.editor.PostNoteTask

class PostNoteService : IntentService("PostNoteService") {

    companion object{
        const val tag = "PostNoteService"
        const val EXTRA_NOTE_TASK = "jp.panta.misskeyandroidclient.EXTRA_NOTE_TASK"
    }

    override fun onHandleIntent(intent: Intent?) {
        val noteTask = intent?.getSerializableExtra(EXTRA_NOTE_TASK) as PostNoteTask?
        if(noteTask == null){
            Log.e(tag, "EXTRA_NOTE_TASKがnullです")
            return
        }
        val miApplication = applicationContext as MiApplication
        val ci = miApplication.currentConnectionInstanceLiveData.value
        if(ci == null){
            Log.e(tag, "ConnectionInstanceの取得に失敗しました")
            return
        }
        val uploader = OkHttpDriveFileUploader(ci, GsonBuilder().create())
        val createNote = noteTask.execute(uploader)
        if(createNote == null){
            Log.d(tag, "ファイルのアップロードに失敗しました")
            return
        }

        Log.d(tag, "createNote: $createNote")
        val result = miApplication.misskeyAPIService?.create(createNote)?.execute()

        if(result?.code() == 200){
            Log.d(tag, "ノートの投稿に成功しました")
        }else{
            Log.d(tag, "ノートの投稿に失敗しました")
        }
    }

}
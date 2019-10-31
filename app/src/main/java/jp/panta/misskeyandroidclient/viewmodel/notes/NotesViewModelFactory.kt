package jp.panta.misskeyandroidclient.viewmodel.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import jp.panta.misskeyandroidclient.MiApplication
import jp.panta.misskeyandroidclient.model.auth.ConnectionInstance
import java.lang.ClassCastException

class NotesViewModelFactory(private val connectionInstance: ConnectionInstance, private val miApplication: MiApplication) : ViewModelProvider.Factory{
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if(modelClass == NotesViewModel::class.java){
            return NotesViewModel(connectionInstance, miApplication.misskeyAPIService!!) as T
        }
        return throw ClassCastException("知らないこだなぁ～？？？")
    }
}
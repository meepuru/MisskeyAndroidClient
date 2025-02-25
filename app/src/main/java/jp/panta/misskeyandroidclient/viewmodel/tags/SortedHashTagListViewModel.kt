package jp.panta.misskeyandroidclient.viewmodel.tags

import androidx.lifecycle.*
import net.pantasystem.milktea.common.throwIfHasError
import net.pantasystem.milktea.model.hashtag.HashTag
import net.pantasystem.milktea.api.misskey.hashtag.RequestHashTagList
import jp.panta.misskeyandroidclient.viewmodel.MiCore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.io.Serializable

class SortedHashTagListViewModel(
    val miCore: MiCore,
    val conditions: Conditions
) : ViewModel(){

    @Suppress("UNCHECKED_CAST")
    class Factory(
        val miCore: MiCore,
        val conditions: Conditions
    ) : ViewModelProvider.Factory{
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SortedHashTagListViewModel(
                miCore,
                conditions
            ) as T
        }
    }

    data class Conditions(
        val sort: String,
        val isAttachedToUserOnly: Boolean? = null,
        val isAttachedToLocalUserOnly: Boolean? = null,
        val isAttachedToRemoteUserOnly: Boolean? = null
    ): Serializable

    val hashTags = object : MediatorLiveData<List<HashTag>>(){

        override fun onActive() {
            super.onActive()

            if(value.isNullOrEmpty()){
                load()
            }
        }

    }

    val isLoading = MutableLiveData<Boolean>()

    init{
        miCore.getAccountStore().observeCurrentAccount.filterNotNull().flowOn(Dispatchers.IO).onEach {
            load()
        }.launchIn(viewModelScope)
    }
    fun load(){
        val account = miCore.getAccountStore().currentAccount
            ?:return
        isLoading.value = true
        val i = runCatching { account.getI(miCore.getEncryption()) }.getOrNull()
        if(i == null){
            isLoading.value = false
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                miCore.getMisskeyAPIProvider().get(account).getHashTagList(
                    RequestHashTagList(
                        i = i,
                        sort = conditions.sort
                    )
                ).throwIfHasError()
            }.onSuccess { response ->
                hashTags.postValue(response.body())
            }
            isLoading.postValue(false)
        }
    }
}
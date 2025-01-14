package net.pantasystem.milktea.channel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import net.pantasystem.milktea.data.infrastructure.channel.impl.ChannelListType
import net.pantasystem.milktea.data.infrastructure.channel.impl.ChannelPagingModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.pantasystem.milktea.common.Logger
import net.pantasystem.milktea.common.PageableState
import net.pantasystem.milktea.common.paginator.PreviousPagingController
import net.pantasystem.milktea.model.account.AccountRepository
import net.pantasystem.milktea.model.account.AccountStore
import net.pantasystem.milktea.model.account.page.Pageable
import net.pantasystem.milktea.model.account.page.newPage
import net.pantasystem.milktea.model.channel.Channel
import net.pantasystem.milktea.model.channel.ChannelRepository
import javax.inject.Inject

@HiltViewModel
class ChannelViewModel @Inject constructor(
    val accountStore: AccountStore,
    private val channelRepository: ChannelRepository,
    private val accountRepository: AccountRepository,
    channelPagingModelFactory: ChannelPagingModel.ModelAssistedFactory,
    loggerFactory: Logger.Factory,
) : ViewModel() {

    val logger: Logger by lazy {
        loggerFactory.create("ChannelViewModel")
    }

    private val channelPagingModelHolder = ChannelPagingModelHolder(channelPagingModelFactory)


    @OptIn(FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun getObservable(key: PagingModelKey): Flow<PageableState<List<Channel>>> {
        return suspend {
            channelPagingModelHolder.get(key)
        }.asFlow().flatMapLatest {
            it.observeChannels()
        }
    }


    fun loadPrevious(key: PagingModelKey) {
        viewModelScope.launch(Dispatchers.IO) {
            val model = channelPagingModelHolder.get(key)
            PreviousPagingController(
                model,
                model,
                model,
                model,
            ).loadPrevious()
        }
    }

    fun clearAndLoad(key: PagingModelKey) {
        viewModelScope.launch(Dispatchers.IO) {
            val model = channelPagingModelHolder.get(key)
            model.clear()
            PreviousPagingController(
                model,
                model,
                model,
                model,
            ).loadPrevious()
        }
    }

    fun follow(channelId: Channel.Id) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                channelRepository.follow(channelId)
            }.onFailure {
                logger.info("follow error:$channelId", e = it)
            }
        }
    }

    fun unFollow(channelId: Channel.Id) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                channelRepository.unFollow(channelId)
            }.onFailure {
                logger.info("unFollow error:$channelId", e = it)
            }
        }
    }

    fun toggleTab(channelId: Channel.Id) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val account = accountRepository.get(channelId.accountId)
                val channel = channelRepository.findOne(channelId).getOrThrow()
                val page = account.newPage(
                    Pageable.ChannelTimeline(channelId = channelId.channelId),
                    channel.name
                )
                val first =
                    account.pages.firstOrNull { (it.pageable() as? Pageable.ChannelTimeline)?.channelId == channelId.channelId }
                if (first == null) {
                    accountStore.addPage(page)
                } else {
                    accountStore.removePage(first)
                }
            }
        }
    }
}

data class PagingModelKey(val accountId: Long, val type: ChannelListType)

class ChannelPagingModelHolder(
    private val channelPagingModelFactory: ChannelPagingModel.ModelAssistedFactory,
) {

    private val lock = Mutex()
    private var modelMap = mapOf<PagingModelKey, ChannelPagingModel>()
    suspend fun get(key: PagingModelKey): ChannelPagingModel {
        lock.withLock {
            var model = modelMap[key]
            if (model == null) {
                model = channelPagingModelFactory.create(key.accountId, key.type)
                modelMap = modelMap.toMutableMap().also {
                    it[key] = model
                }
            }
            return model
        }
    }
}
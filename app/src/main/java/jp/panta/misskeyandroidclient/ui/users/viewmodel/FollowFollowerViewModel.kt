package jp.panta.misskeyandroidclient.ui.users.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import jp.panta.misskeyandroidclient.util.eventbus.EventBus
import jp.panta.misskeyandroidclient.viewmodel.MiCore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.pantasystem.milktea.api.misskey.users.RequestUser
import net.pantasystem.milktea.api.misskey.v10.MisskeyAPIV10
import net.pantasystem.milktea.api.misskey.v10.RequestFollowFollower
import net.pantasystem.milktea.api.misskey.v11.MisskeyAPIV11
import net.pantasystem.milktea.common.Encryption
import net.pantasystem.milktea.common.Logger
import net.pantasystem.milktea.data.infrastructure.notes.NoteDataSourceAdder
import net.pantasystem.milktea.data.infrastructure.toUser
import net.pantasystem.milktea.model.account.Account
import net.pantasystem.milktea.model.user.User
import net.pantasystem.milktea.model.user.UserDataSource

class FollowFollowerViewModel(
    val userId: User.Id,
    val type: Type,
    private val miCore: MiCore,
    private val noteDataSourceAdder: NoteDataSourceAdder = NoteDataSourceAdder(
        miCore.getUserDataSource(),
        miCore.getNoteDataSource(),
        miCore.getFilePropertyDataSource()
    )
) : ViewModel(), ShowUserDetails {
    @Suppress("UNCHECKED_CAST")
    class Factory(
        val userId: User.Id,
        val type: Type,
        val miCore: MiCore,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return FollowFollowerViewModel(userId, type, miCore) as T
        }

    }

    enum class Type {
        FOLLOWING,
        FOLLOWER
    }

    interface Paginator {
        suspend fun next(): List<User.Detail>
        suspend fun init()
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    class DefaultPaginator(
        val account: Account,
        val misskeyAPI: MisskeyAPIV11,
        val userId: User.Id,
        val type: Type,
        val encryption: Encryption,
        val noteDataSourceAdder: NoteDataSourceAdder,
        val userDataSource: UserDataSource,
        private val logger: Logger?

    ) : Paginator {


        private val lock = Mutex()
        private val api =
            if (type == Type.FOLLOWER) misskeyAPI::followers else misskeyAPI::following
        private var nextId: String? = null

        override suspend fun next(): List<User.Detail> {
            lock.withLock {
                logger?.debug("next: $nextId")
                val res = api.invoke(
                    RequestUser(
                        account.getI(encryption),
                        userId = userId.id,
                        untilId = nextId
                    )
                ).body()
                    ?: return emptyList()
                nextId = res.last().id
                require(nextId != null)
                return res.mapNotNull {
                    it.followee ?: it.follower
                }.map { userDTO ->
                    userDTO.pinnedNotes?.forEach { noteDTO ->
                        noteDataSourceAdder.addNoteDtoToDataSource(account, noteDTO)
                    }
                    (userDTO.toUser(account, true) as User.Detail).also {
                        userDataSource.add(it)
                    }
                }
            }
        }

        override suspend fun init() {
            lock.withLock {
                nextId = null
            }
        }
    }


    @Suppress("BlockingMethodInNonBlockingContext")
    class V10Paginator(
        val account: Account,
        private val misskeyAPIV10: MisskeyAPIV10,
        val userId: User.Id,
        val type: Type,
        val encryption: Encryption,
        val noteDataSourceAdder: NoteDataSourceAdder,
        val userDataSource: UserDataSource
    ) : Paginator {
        private val lock = Mutex()
        private var nextId: String? = null
        private val api =
            if (type == Type.FOLLOWER) misskeyAPIV10::followers else misskeyAPIV10::following

        override suspend fun next(): List<User.Detail> {
            lock.withLock {
                val res = api.invoke(
                    RequestFollowFollower(
                        i = account.getI(encryption),
                        cursor = nextId,
                        userId = userId.id
                    )
                ).body() ?: return emptyList()
                nextId = res.next
                return res.users.map { userDTO ->
                    userDTO.pinnedNotes?.forEach {
                        noteDataSourceAdder.addNoteDtoToDataSource(account, it)
                    }
                    (userDTO.toUser(account, true) as User.Detail).also {
                        userDataSource.add(it)
                    }
                }
            }
        }

        override suspend fun init() {
            lock.withLock {
                nextId = null
            }
        }
    }


    val logger = miCore.loggerFactory.create("FollowFollowerViewModel")

    val accountRepository = miCore.getAccountRepository()
    val userRepository = miCore.getUserRepository()
    private val misskeyAPIProvider = miCore.getMisskeyAPIProvider()
    private val encryption = miCore.getEncryption()
    private val userDataSource: UserDataSource = miCore.getUserDataSource()

    val isInitializing = MutableLiveData(false)

    private val userViewDataFactory = miCore.userViewDataFactory()

    @FlowPreview
    @ExperimentalCoroutinesApi
    val users = MutableLiveData<List<UserViewData>>()

    @FlowPreview
    @ExperimentalCoroutinesApi
    private var mUsers: List<UserViewData> = emptyList()
        set(value) {
            field = value
            users.postValue(value)
        }

    private var mIsLoading: Boolean = false


    @ExperimentalCoroutinesApi
    @FlowPreview
    fun loadInit() = viewModelScope.launch(Dispatchers.IO) {
        isInitializing.postValue(true)
        getPaginator().init()
        mUsers = emptyList()
        loadOld().join()
        isInitializing.postValue(false)

    }


    @FlowPreview
    @ExperimentalCoroutinesApi
    fun loadOld() = viewModelScope.launch(Dispatchers.IO) {
        logger.debug("loadOld")
        if (mIsLoading) return@launch
        mIsLoading = true
        runCatching {
            val list = getPaginator().next().map {
                userViewDataFactory.create(it, viewModelScope, Dispatchers.IO)
            }
            mUsers = mUsers.toMutableList().also {
                it.addAll(list)
            }
        }.onFailure {
            logger.error("load error", e = it)
        }
        mIsLoading = false

        isInitializing.postValue(false)
    }


    val showUserEventBus = EventBus<User.Id>()

    override fun show(userId: User.Id?) {
        showUserEventBus.event = userId
    }


    private var mAccount: Account? = null
    private var mPaginator: Paginator? = null

    private suspend fun getPaginator(): Paginator {
        if (mPaginator != null) {
            return mPaginator
                ?: throw IllegalStateException("paginator is null")
        }
        logger.debug("paginator 生成")

        if (mAccount == null) {
            mAccount = accountRepository.get(userId.accountId)
        }
        mPaginator = mAccount?.let { account ->
            val api = misskeyAPIProvider.get(account.instanceDomain)
            if (api is MisskeyAPIV10) {
                V10Paginator(
                    account,
                    api,
                    userId,
                    type,
                    encryption,
                    noteDataSourceAdder,
                    userDataSource
                )
            } else {
                DefaultPaginator(
                    account,
                    api as MisskeyAPIV11,
                    userId,
                    type,
                    encryption,
                    noteDataSourceAdder,
                    userDataSource,
                    miCore.loggerFactory.create("DefaultPaginator")
                )
            }
        }
        require(mPaginator != null)
        return mPaginator!!
    }

}
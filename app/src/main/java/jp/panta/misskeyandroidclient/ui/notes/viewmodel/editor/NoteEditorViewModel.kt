package jp.panta.misskeyandroidclient.ui.notes.viewmodel.editor

import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import net.pantasystem.milktea.data.model.CreateNoteTaskExecutor
import net.pantasystem.milktea.model.account.Account
import net.pantasystem.milktea.model.account.AccountStore
import net.pantasystem.milktea.data.model.api.Version
import net.pantasystem.milktea.model.channel.Channel
import net.pantasystem.milktea.model.drive.DriveFileRepository
import net.pantasystem.milktea.model.drive.FileProperty
import net.pantasystem.milktea.model.drive.FilePropertyDataSource
import net.pantasystem.milktea.model.emoji.Emoji
import net.pantasystem.milktea.model.file.AppFile
import net.pantasystem.milktea.model.file.toFile
import net.pantasystem.milktea.model.instance.MetaRepository
import net.pantasystem.milktea.data.model.notes.*
import net.pantasystem.milktea.model.notes.draft.DraftNote
import net.pantasystem.milktea.model.notes.draft.DraftNoteDao
import net.pantasystem.milktea.model.user.User
import jp.panta.misskeyandroidclient.util.eventbus.EventBus
import jp.panta.misskeyandroidclient.viewmodel.MiCore
import jp.panta.misskeyandroidclient.ui.users.viewmodel.UserViewData
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock
import net.pantasystem.milktea.model.notes.isLocalOnly
import net.pantasystem.milktea.model.notes.task
import net.pantasystem.milktea.model.notes.toCreateNote
import net.pantasystem.milktea.model.notes.toDraftPoll
import net.pantasystem.milktea.model.notes.type
import java.io.IOException
import java.util.*
import javax.inject.Inject

@HiltViewModel
class NoteEditorViewModel @Inject constructor(
    private val draftNoteDao: net.pantasystem.milktea.model.notes.draft.DraftNoteDao,
    loggerFactory: net.pantasystem.milktea.common.Logger.Factory,
    private val miCore: MiCore,
    private val noteRepository: net.pantasystem.milktea.model.notes.NoteRepository,
    private val filePropertyDataSource: net.pantasystem.milktea.model.drive.FilePropertyDataSource,
    private val metaRepository: net.pantasystem.milktea.model.instance.MetaRepository,
    private val driveFileRepository: net.pantasystem.milktea.model.drive.DriveFileRepository,
    private val accountStore: net.pantasystem.milktea.model.account.AccountStore,
    private val createNoteTaskExecutor: CreateNoteTaskExecutor
) : ViewModel() {

    private val dispatcher: CoroutineDispatcher = Dispatchers.IO

    private val logger = loggerFactory.create("NoteEditorViewModel")

    private val _state = MutableStateFlow(net.pantasystem.milktea.model.notes.NoteEditingState())
    val state: StateFlow<net.pantasystem.milktea.model.notes.NoteEditingState> = _state

    val text = _state.map {
        it.text
    }.stateIn(viewModelScope, started = SharingStarted.Lazily, initialValue = null)
    val cw = _state.map {
        it.cw
    }.stateIn(viewModelScope, started = SharingStarted.Lazily, initialValue = null)

    private val currentAccount = MutableLiveData<net.pantasystem.milktea.model.account.Account>().apply {
        accountStore.observeCurrentAccount.onEach {
            this.postValue(it)
        }.launchIn(viewModelScope + dispatcher)
    }

    @FlowPreview
    @ExperimentalCoroutinesApi
    val currentUser: StateFlow<UserViewData?> =
        accountStore.state.map { it.currentAccount }.filterNotNull().map {
            val userId = net.pantasystem.milktea.model.user.User.Id(it.accountId, it.remoteId)
            UserViewData(
                userId,
                miCore,
                viewModelScope,
                dispatcher
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, null)


    //val replyToNoteId = MutableLiveData<Note.Id>(replyId)
    val reply = _state.map {
        it.replyId?.let { noteId ->
            runCatching {
                noteRepository.find(noteId)
            }.getOrNull()
        }
    }.stateIn(viewModelScope + Dispatchers.IO, started = SharingStarted.Lazily, initialValue = null)

    val renoteId = _state.map {
        it.renoteId
    }.asLiveData()

    val hasCw = _state.map {
        it.hasCw
    }.asLiveData()


    val maxTextLength = accountStore.observeCurrentAccount.filterNotNull().map {
        metaRepository.get(it.instanceDomain)?.maxNoteTextLength ?: 1500
    }.stateIn(viewModelScope + Dispatchers.IO, started = SharingStarted.Lazily, initialValue = 1500)

    val textRemaining = combine(maxTextLength, state.map { it.text }) { max, t ->
        max - (t?.codePointCount(0, t.length) ?: 0)
    }.stateIn(viewModelScope + Dispatchers.IO, started = SharingStarted.Lazily, initialValue = 1500)

    val maxFileCount = accountStore.observeCurrentAccount.filterNotNull().mapNotNull {
        metaRepository.get(it.instanceDomain)?.getVersion()
    }.map {
        if (it >= Version("12.100.2")) {
            16
        } else {
            4
        }
    }.stateIn(viewModelScope + Dispatchers.IO, started = SharingStarted.Eagerly, initialValue = 4)


    val files = _state.map {
        it.files
    }.asLiveData()

    val totalImageCount = _state.map {
        it.totalFilesCount
    }.asLiveData()


    val isPostAvailable = _state.map {
        it.checkValidate(textMaxLength = maxTextLength.value, maxFileCount = maxFileCount.value)
    }.asLiveData()

    val visibility = _state.map {
        it.visibility
    }.stateIn(viewModelScope, SharingStarted.Eagerly, initialValue = net.pantasystem.milktea.model.notes.Visibility.Public(false))


    val isLocalOnly = _state.map {
        it.visibility.isLocalOnly()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)


    val isLocalOnlyEnabled = _state.map {
        it.visibility is net.pantasystem.milktea.model.notes.CanLocalOnly
    }.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val reservationPostingAt = _state.map {
        it.reservationPostingAt
    }.map { instant ->
        instant?.toEpochMilliseconds()?.let {
            Date(it)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)


    val showVisibilitySelectionEvent = EventBus<Unit>()
    private val visibilitySelectedEvent = EventBus<Unit>()


    @FlowPreview
    @ExperimentalCoroutinesApi
    val address = visibility.map {
        it as? net.pantasystem.milktea.model.notes.Visibility.Specified
    }.map {
        it?.visibleUserIds?.map { uId ->
            setUpUserViewData(uId)
        } ?: emptyList()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())


    @FlowPreview
    @ExperimentalCoroutinesApi
    private fun setUpUserViewData(userId: net.pantasystem.milktea.model.user.User.Id): UserViewData {
        return UserViewData(userId, miCore, viewModelScope, dispatcher)
    }

    val isSpecified = _state.map {
        it.isSpecified
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val poll = _state.map {
        it.poll
    }.distinctUntilChanged()
        .stateIn(viewModelScope, started = SharingStarted.Lazily, initialValue = null)

    //val noteTask = MutableLiveData<PostNoteTask>()
    val isPost = EventBus<Boolean>()

    val showPollDatePicker = EventBus<Unit>()
    val showPollTimePicker = EventBus<Unit>()


    val isSaveNoteAsDraft = EventBus<Long?>()

    fun setRenoteTo(noteId: net.pantasystem.milktea.model.notes.Note.Id?) {
        _state.value = _state.value.changeRenoteId(noteId)
    }

    fun setReplyTo(noteId: net.pantasystem.milktea.model.notes.Note.Id?) {
        _state.value = _state.value.changeReplyTo(noteId)
    }

    fun setDraftNote(note: net.pantasystem.milktea.model.notes.draft.DraftNote?) {
        _state.value = _state.value.setDraftNote(note)
    }

    init {
        accountStore.observeCurrentAccount.filterNotNull().onEach {
            _state.value = runCatching {
                _state.value.setAccount(it)
            }.getOrElse {
                net.pantasystem.milktea.model.notes.NoteEditingState()
            }
        }.launchIn(viewModelScope + Dispatchers.IO)

        accountStore.observeCurrentAccount.filterNotNull().onEach {
            val v = miCore.getSettingStore().getNoteVisibility(it.accountId)
            _state.value = _state.value.setVisibility(v)

        }.launchIn(viewModelScope + Dispatchers.IO)

    }

    fun changeText(text: String) {
        _state.value = _state.value.changeText(text)
    }

    fun addPollChoice() {
        _state.value = _state.value.addPollChoice()
    }

    fun changePollChoice(id: UUID, text: String) {
        _state.value = _state.value.updatePollChoice(id, text)
    }

    fun removePollChoice(id: UUID) {
        _state.value = _state.value.removePollChoice(id)
    }

    fun updateState(state: net.pantasystem.milktea.model.notes.NoteEditingState) {
        _state.value = state
    }

    fun togglePollMultiple() {
        _state.value = state.value.copy(
            poll = state.value.poll?.toggleMultiple()
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    fun post() {
        currentAccount.value?.let { account ->
            viewModelScope.launch(Dispatchers.IO) {

                val reservationPostingAt = _state.value.reservationPostingAt
                if (reservationPostingAt == null || reservationPostingAt <= Clock.System.now()) {
                    val createNote = _state.value.toCreateNote(account)
                    createNoteTaskExecutor.dispatch(createNote.task(noteRepository))
                } else {
                    runCatching {
                        val dfNote = toDraftNote()

                        val result = draftNoteDao.fullInsert(dfNote)
                        dfNote.draftNoteId = result
                        miCore.getNoteReservationPostExecutor().register(dfNote)
                    }.onFailure {
                        logger.error("登録失敗", it)
                    }
                }
                withContext(Dispatchers.Main) {
                    isPost.event = true
                }
            }

        }

    }

    fun toggleNsfw(appFile: net.pantasystem.milktea.model.file.AppFile) {
        when (appFile) {
            is net.pantasystem.milktea.model.file.AppFile.Local -> {
                _state.value = state.value.toggleFileSensitiveStatus(appFile)
            }
            is net.pantasystem.milktea.model.file.AppFile.Remote -> {
                viewModelScope.launch(Dispatchers.IO) {
                    runCatching {
                        driveFileRepository.toggleNsfw(appFile.id)
                    }
                }
            }
        }

    }

    fun add(file: net.pantasystem.milktea.model.file.AppFile) {
        val files = files.value?.toMutableList()
            ?: mutableListOf()
        files.add(
            file
        )
        _state.value = _state.value.addFile(file)
    }


    private fun addAllFileProperty(fpList: List<net.pantasystem.milktea.model.drive.FileProperty>) {
        val files = state.value.files.toMutableList()
        files.addAll(fpList.map {
            net.pantasystem.milktea.model.file.AppFile.Remote(it.id)
        })
        _state.value = _state.value.copy(
            files = files
        )
    }

    fun addFilePropertyFromIds(ids: List<net.pantasystem.milktea.model.drive.FileProperty.Id>) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                filePropertyDataSource.findIn(ids)
            }.onSuccess {
                addAllFileProperty(it)
            }
        }
    }

    fun removeFileNoteEditorData(file: net.pantasystem.milktea.model.file.AppFile) {
        _state.value = _state.value.removeFile(file)
    }


    fun fileTotal(): Int {
        return files.value?.size ?: 0
    }


    fun changeCwEnabled() {
        _state.value = _state.value.toggleCw()
        logger.debug("cw:${cw.value}")
    }

    fun enablePoll() {
        _state.value = _state.value.togglePoll()

    }

    fun disablePoll() {
        _state.value = _state.value.togglePoll()
    }

    fun showVisibilitySelection() {
        showVisibilitySelectionEvent.event = Unit
    }

    fun setText(text: String) {
        _state.value = _state.value.changeText(text)
    }

    fun setCw(text: String?) {
        _state.value = _state.value.changeCw(text)
    }

    fun setVisibility(visibility: net.pantasystem.milktea.model.notes.Visibility) {
        logger.debug("公開範囲がセットされた:$visibility")
        _state.value = _state.value.setVisibility(visibility)
        this.visibilitySelectedEvent.event = Unit
    }

    fun setChannelId(channelId: net.pantasystem.milktea.model.channel.Channel.Id?) {
        _state.value = _state.value.setChannelId(channelId)
    }

    fun toggleReservationAt() {
        _state.value = _state.value.copy(
            reservationPostingAt = if (_state.value.reservationPostingAt == null) Clock.System.now() else null
        )
    }


    @FlowPreview
    @ExperimentalCoroutinesApi
    fun setAddress(added: List<net.pantasystem.milktea.model.user.User.Id>, removed: List<net.pantasystem.milktea.model.user.User.Id>) {
        val list = ((visibility.value as? net.pantasystem.milktea.model.notes.Visibility.Specified)?.visibleUserIds
            ?: emptyList()).toMutableList()
        list.addAll(
            added
        )

        list.removeAll {
            removed.any()
        }

        _state.value = _state.value.copy(
            visibility = net.pantasystem.milktea.model.notes.Visibility.Specified(list)
        )
    }


    fun addMentionUsers(users: List<net.pantasystem.milktea.model.user.User>, pos: Int): Int {
        val userNames = users.map {
            it.getDisplayUserName()
        }
        return addMentionUserNames(userNames, pos)
    }

    fun addMentionUserNames(userNames: List<String>, pos: Int): Int {
        val result = _state.value.addMentionUserNames(userNames, pos)
        _state.value = result.state
        return result.cursorPos
    }

    fun addEmoji(emoji: net.pantasystem.milktea.model.emoji.Emoji, pos: Int): Int {
        return addEmoji(":${emoji.name}:", pos)
    }

    fun addEmoji(emoji: String, pos: Int): Int {
        val builder = StringBuilder(_state.value.text ?: "")
        builder.insert(pos, emoji)
        _state.value = _state.value.changeText(builder.toString())
        logger.debug("position:${pos + emoji.length - 1}")
        return pos + emoji.length
    }

    @FlowPreview
    @ExperimentalCoroutinesApi
    fun toDraftNote(): net.pantasystem.milktea.model.notes.draft.DraftNote {
        return net.pantasystem.milktea.model.notes.draft.DraftNote(
            accountId = currentAccount.value?.accountId!!,
            text = _state.value.text,
            cw = _state.value.cw,
            visibleUserIds = address.value.mapNotNull {
                it.userId?.id ?: it.user.value?.id?.id
            },
            draftPoll = poll.value?.toDraftPoll(),
            visibility = visibility.value.type(),
            localOnly = visibility.value.isLocalOnly(),
            renoteId = _state.value.renoteId?.noteId,
            replyId = _state.value.replyId?.noteId,
            files = files.value?.map {
                it.toFile()
            },
            reservationPostingAt = _state.value.reservationPostingAt?.toEpochMilliseconds()?.let {
                Date(it)
            },
            channelId = _state.value.channelId,
        ).apply {
            setDraftNote(this)
        }
    }

    @ExperimentalCoroutinesApi
    @FlowPreview
    fun saveDraft() {
        if (!canSaveDraft()) {
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dfNote = toDraftNote()

                try {
                    isSaveNoteAsDraft.event = draftNoteDao.fullInsert(dfNote)
                } catch (e: Exception) {
                    logger.error("下書き書き込み中にエラー発生：失敗してしまった", e)
                }
            } catch (e: IOException) {

            } catch (e: NullPointerException) {
                logger.error("下書き保存に失敗した", e)

            } catch (e: Throwable) {
                logger.error("下書き保存に失敗した", e)

            }

        }
    }

    @ExperimentalCoroutinesApi
    @FlowPreview
    fun canSaveDraft(): Boolean {
        return !(_state.value.text.isNullOrBlank()
                && files.value.isNullOrEmpty()
                && poll.value?.choices.isNullOrEmpty()
                && address.value.isNullOrEmpty())
    }


    @FlowPreview
    @ExperimentalCoroutinesApi
    fun clear() {
        _state.value = _state.value.clear()
    }


}
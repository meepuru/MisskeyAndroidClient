package net.pantasystem.milktea.data.model.drive

import net.pantasystem.milktea.data.api.misskey.MisskeyAPIProvider
import net.pantasystem.milktea.data.model.*

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.pantasystem.milktea.api.misskey.drive.FilePropertyDTO
import net.pantasystem.milktea.api.misskey.drive.RequestFile
import net.pantasystem.milktea.common.Encryption
import net.pantasystem.milktea.common.PageableState
import net.pantasystem.milktea.common.StateContent
import net.pantasystem.milktea.data.api.misskey.throwIfHasError
import net.pantasystem.milktea.model.drive.Directory
import net.pantasystem.milktea.model.drive.FileProperty
import net.pantasystem.milktea.model.drive.FilePropertyDataSource
import retrofit2.Response


class FilePropertyPagingStore(
    private var currentDirectoryId: String?,
    private val getAccount: suspend () -> net.pantasystem.milktea.model.account.Account,
    misskeyAPIProvider: MisskeyAPIProvider,
    filePropertyDataSource: FilePropertyDataSource,
    encryption: Encryption,

    ) {

    private val filePropertyPagingImpl = FilePropertyPagingImpl(
        misskeyAPIProvider,
        {
            getAccount.invoke()
        },
        {
            currentDirectoryId
        },
        encryption,
        filePropertyDataSource
    )

    private val previousPagingController: PreviousPagingController<FilePropertyDTO, FileProperty.Id> = PreviousPagingController(
        filePropertyPagingImpl,
        filePropertyPagingImpl,
        filePropertyPagingImpl,
        filePropertyPagingImpl
    )

    val state = this.filePropertyPagingImpl.state

    val isLoading: Boolean get() = this.filePropertyPagingImpl.mutex.isLocked

    suspend fun loadPrevious() {
        previousPagingController.loadPrevious()
    }

    suspend fun clear() {
        this.filePropertyPagingImpl.mutex.withLock {
            this.filePropertyPagingImpl.setState(PageableState.Loading.Init())
        }
    }

    suspend fun setCurrentDirectory(directory: Directory?) {
        this.clear()
        this.currentDirectoryId = directory?.id
    }

}


class FilePropertyPagingImpl(
    private val misskeyAPIProvider: MisskeyAPIProvider,
    private val getAccount: suspend ()-> net.pantasystem.milktea.model.account.Account,
    private val getCurrentFolderId: ()-> String?,
    private val encryption: Encryption,
    private val filePropertyDataSource: FilePropertyDataSource
) : PaginationState<FileProperty.Id>,
    IdGetter<String>, PreviousLoader<FilePropertyDTO>,
    EntityConverter<FilePropertyDTO, FileProperty.Id>,
    StateLocker
{

    private val _state = MutableStateFlow<PageableState<List<FileProperty.Id>>>(PageableState.Fixed(
        StateContent.NotExist()))
    override val state: Flow<PageableState<List<FileProperty.Id>>>
        get() = _state

    override val mutex: Mutex = Mutex()

    override fun setState(state: PageableState<List<FileProperty.Id>>) {
        _state.value = state
    }

    override fun getState(): PageableState<List<FileProperty.Id>> {
        return _state.value
    }

    override suspend fun getSinceId(): String? {
        return (getState().content as? StateContent.Exist<List<FileProperty.Id>>)?.rawContent?.firstOrNull()?.fileId
    }

    override suspend fun getUntilId(): String? {
        return (getState().content as? StateContent.Exist<List<FileProperty.Id>>)?.rawContent?.lastOrNull()?.fileId
    }

    override suspend fun loadPrevious(): Response<List<FilePropertyDTO>> {
        return misskeyAPIProvider.get(getAccount.invoke().instanceDomain).getFiles(
            RequestFile(
                folderId = getCurrentFolderId.invoke(),
                untilId = this.getUntilId(),
                i = getAccount.invoke().getI(encryption),
                limit = 20
            )
        ).throwIfHasError()
    }

    override suspend fun convertAll(list: List<FilePropertyDTO>): List<FileProperty.Id> {
        val entities = list.map {
            it.toFileProperty(getAccount.invoke())
        }
        filePropertyDataSource.addAll(entities)
        return entities.map {
            it.id
        }
    }

}


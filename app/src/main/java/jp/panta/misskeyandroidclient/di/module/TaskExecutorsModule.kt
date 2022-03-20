package jp.panta.misskeyandroidclient.di.module

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import jp.panta.misskeyandroidclient.Logger
import jp.panta.misskeyandroidclient.model.CreateGalleryTaskExecutor
import jp.panta.misskeyandroidclient.model.CreateNoteTaskExecutor
import jp.panta.misskeyandroidclient.model.TaskExecutorImpl
import kotlinx.coroutines.CoroutineScope
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object TaskExecutorsModule {

    @Provides
    @Singleton
    fun provideNoteCreateTaskExecutor(
        coroutineScope: CoroutineScope,
        loggerFactory: Logger.Factory,
    ): CreateNoteTaskExecutor {
        return CreateNoteTaskExecutor(
            provideTaskExecutor(coroutineScope, loggerFactory)
        )
    }

    @Provides
    @Singleton
    fun provideGalleryPostTaskExecutor(
        coroutineScope: CoroutineScope,
        loggerFactory: Logger.Factory,
    ): CreateGalleryTaskExecutor {
        return CreateGalleryTaskExecutor(
            provideTaskExecutor(coroutineScope, loggerFactory)
        )
    }

    private fun <T> provideTaskExecutor(
        coroutineScope: CoroutineScope,
        loggerFactory: Logger.Factory,
    ): TaskExecutorImpl<T> {
        return TaskExecutorImpl(coroutineScope, loggerFactory.create("CreateNoteTaskExecutor"))
    }

}
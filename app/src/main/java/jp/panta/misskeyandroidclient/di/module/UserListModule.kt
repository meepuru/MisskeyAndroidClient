package jp.panta.misskeyandroidclient.di.module

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.pantasystem.milktea.model.list.UserListRepository
import net.pantasystem.milktea.data.infrastructure.list.impl.UserListRepositoryWebAPIImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class UserListBindsModule {

    @Binds
    @Singleton
    abstract fun provideUserListRepository(
        impl: UserListRepositoryWebAPIImpl
    ) : UserListRepository
}



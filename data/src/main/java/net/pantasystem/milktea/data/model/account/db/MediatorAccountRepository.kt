package net.pantasystem.milktea.data.model.account.db

import net.pantasystem.milktea.model.account.Account
import net.pantasystem.milktea.model.account.AccountNotFoundException
import net.pantasystem.milktea.model.account.AccountRepository

/**
 * データベースの内容をメモリにキャッシュしデータベースを制御する。
 * Writeは遅くなるがReadは高速化することが期待できる。
 */
class MediatorAccountRepository(
    private val roomAccountRepository: RoomAccountRepository
) : net.pantasystem.milktea.model.account.AccountRepository {

    private var mAccounts: List<net.pantasystem.milktea.model.account.Account> = listOf()

    override suspend fun add(account: net.pantasystem.milktea.model.account.Account, isUpdatePages: Boolean): net.pantasystem.milktea.model.account.Account {
        return roomAccountRepository.add(account, isUpdatePages).also {
            mAccounts = roomAccountRepository.findAll()
        }
    }

    override suspend fun delete(account: net.pantasystem.milktea.model.account.Account) {
        return roomAccountRepository.delete(account).also {
            mAccounts = roomAccountRepository.findAll()
        }
    }

    override suspend fun findAll(): List<net.pantasystem.milktea.model.account.Account> {
        if(mAccounts.isEmpty()) {
            mAccounts = roomAccountRepository.findAll()
        }
        return mAccounts
    }


    override suspend fun get(accountId: Long): net.pantasystem.milktea.model.account.Account {
        return findAll().firstOrNull {
            it.accountId == accountId
        }?: throw net.pantasystem.milktea.model.account.AccountNotFoundException()
    }

    override suspend fun getCurrentAccount(): net.pantasystem.milktea.model.account.Account {
        return roomAccountRepository.getCurrentAccount()
    }

    override suspend fun setCurrentAccount(account: net.pantasystem.milktea.model.account.Account): net.pantasystem.milktea.model.account.Account {
        return roomAccountRepository.setCurrentAccount(account).also {
            mAccounts = findAll()
        }
    }

    override fun addEventListener(listener: net.pantasystem.milktea.model.account.AccountRepository.Listener) {
        roomAccountRepository.addEventListener(listener)
    }

    override fun removeEventListener(listener: net.pantasystem.milktea.model.account.AccountRepository.Listener) {
        roomAccountRepository.removeEventListener(listener)
    }
}
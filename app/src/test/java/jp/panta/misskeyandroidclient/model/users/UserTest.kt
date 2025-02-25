package jp.panta.misskeyandroidclient.model.users

import junit.framework.TestCase
import net.pantasystem.milktea.model.account.Account
import net.pantasystem.milktea.model.user.User

class UserTest : TestCase() {

    fun testGetProfileUrl() {

        val user = User.Simple(
            id = User.Id(0, "id"),
            avatarUrl = "",
            emojis = emptyList(),
            host = null,
            isBot = false,
            isCat = false,
            name = "Panta",
            userName = "Panta",
            nickname = null
        )

        val profileUrl = user.getProfileUrl(
            Account(
                instanceDomain = "https://example.com",
                encryptedToken = "",
                remoteId = "",
                userName = "",
                instanceType = Account.InstanceType.MISSKEY
            ))
        assertEquals("https://example.com/@Panta", profileUrl)
    }

    fun testGetProfileUrlWhenRemoteHost() {

        val user = User.Simple(
            id = User.Id(0, "id"),
            avatarUrl = "",
            emojis = emptyList(),
            host = "misskey.io",
            isBot = false,
            isCat = false,
            name = "Panta",
            userName = "Panta",
            nickname = null
        )

        val profileUrl = user.getProfileUrl(Account(
            instanceDomain = "https://example.com",
            encryptedToken = "",
            remoteId = "",
            userName = "",
            instanceType = Account.InstanceType.MISSKEY
        ))
        assertEquals("https://example.com/@Panta@misskey.io", profileUrl)
    }
}
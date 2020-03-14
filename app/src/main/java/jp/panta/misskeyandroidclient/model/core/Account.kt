package jp.panta.misskeyandroidclient.model.core

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * @param id 対となるUserのuserId
 */
@Entity
class Account(
    @PrimaryKey(autoGenerate = false)
    val id: String
)
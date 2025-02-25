package net.pantasystem.milktea.api.Instance.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
abstract class MetaDAO{

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(meta: MetaDTO)

    @Delete
    abstract fun delete(meta: MetaDTO)

    @Transaction
    @Query("select * from meta_table where uri = :instanceDomain")
    abstract fun findByInstanceDomain(instanceDomain: String): MetaRelation?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(emojiDTO: EmojiDTO)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertAll(emojis: List<EmojiDTO>)

    @Query("select * from emoji_table where name = :name and instanceDomain = :instanceDomain")
    abstract fun findByNameAndInstanceDomain(name: String, instanceDomain: String) : EmojiDTO

    @Query("select * from emoji_table where instanceDomain = :instanceDomain")
    abstract fun findAllByInstanceDomain(instanceDomain: String) : List<EmojiDTO>

    @Transaction
    @Query("select * from meta_table where uri = :instanceDomain")
    abstract fun observeByInstanceDomain(instanceDomain: String): Flow<MetaRelation?>

}
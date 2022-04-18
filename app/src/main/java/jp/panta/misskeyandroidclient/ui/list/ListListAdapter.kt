package jp.panta.misskeyandroidclient.ui.list

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import jp.panta.misskeyandroidclient.R
import jp.panta.misskeyandroidclient.databinding.ItemListAddUserBinding
import jp.panta.misskeyandroidclient.databinding.ItemListBinding

import net.pantasystem.milktea.model.list.UserList
import net.pantasystem.milktea.model.user.User
import jp.panta.misskeyandroidclient.ui.list.viewmodel.ListListViewModel
import jp.panta.misskeyandroidclient.ui.list.viewmodel.UserListPullPushUserViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi

class ListListAdapter @ExperimentalCoroutinesApi constructor(
    private val listListViewModel: ListListViewModel,
    val lifecycleOwner: LifecycleOwner,
    var onTryToEditCallback: OnTryToEditCallback? = null
) : ListAdapter<net.pantasystem.milktea.model.list.UserList, ListListAdapter.BaseVH>(
    ItemCallback()
){

    abstract class BaseVH(view: View) : RecyclerView.ViewHolder(view){
        abstract fun bind(userList: net.pantasystem.milktea.model.list.UserList)
    }

    inner class VH(val binding: ItemListBinding) : BaseVH(binding.root){
        @OptIn(ExperimentalCoroutinesApi::class)
        override fun bind(userList: net.pantasystem.milktea.model.list.UserList) {
            binding.list = userList
            binding.lifecycleOwner = lifecycleOwner
            binding.tryToEditCallback = onTryToEditCallback
            binding.listListViewModel = listListViewModel
            binding.executePendingBindings()
        }
    }

    inner class AddUserVH(val binding: ItemListAddUserBinding) : BaseVH(binding.root){
        override fun bind(userList: net.pantasystem.milktea.model.list.UserList) {
            binding.userList = userList
            binding.addUserId = addUserId
            binding.pullPushViewModel = pullPushUserViewModel
            binding.executePendingBindings()
        }
    }

    class ItemCallback : DiffUtil.ItemCallback<net.pantasystem.milktea.model.list.UserList>(){
        override fun areContentsTheSame(oldItem: net.pantasystem.milktea.model.list.UserList, newItem: net.pantasystem.milktea.model.list.UserList): Boolean {
            return oldItem == newItem
        }

        override fun areItemsTheSame(oldItem: net.pantasystem.milktea.model.list.UserList, newItem: net.pantasystem.milktea.model.list.UserList): Boolean {
            return oldItem.id == newItem.id
        }
    }

    interface OnTryToEditCallback{

        fun onEdit(userList: net.pantasystem.milktea.model.list.UserList?)
    }

    private var addUserId: net.pantasystem.milktea.model.user.User.Id? = null
    private var pullPushUserViewModel: UserListPullPushUserViewModel? = null

    @ExperimentalCoroutinesApi
    constructor(
        listListViewModel: ListListViewModel,
        lifecycleOwner: LifecycleOwner,
        onTryToEditCallback: OnTryToEditCallback?,
        addUserId: net.pantasystem.milktea.model.user.User.Id,
        pullPushUserViewModel: UserListPullPushUserViewModel
    )
    :this(listListViewModel, lifecycleOwner, onTryToEditCallback){
        this.addUserId = addUserId
        this.pullPushUserViewModel = pullPushUserViewModel
    }

    override fun getItemViewType(position: Int): Int {
        return if(addUserId == null || pullPushUserViewModel == null){
            // ユーザーリストを一覧表示する
            2
        }else{
            // ユーザーをリストに加える pull or push
            1
        }
    }

    override fun onBindViewHolder(holder: BaseVH, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseVH {
        val inflater = LayoutInflater.from(parent.context)
        return when(viewType){
            1 ->{
                val binding = DataBindingUtil.inflate<ItemListAddUserBinding>(inflater, R.layout.item_list_add_user, parent, false)
                AddUserVH(binding)
            }
            2 ->{
                val binding = DataBindingUtil.inflate<ItemListBinding>(inflater, R.layout.item_list, parent, false)
                VH(binding)
            }
            else -> throw IllegalStateException("ViewTypeは1 2しか許可されていません。")
        }
    }
}
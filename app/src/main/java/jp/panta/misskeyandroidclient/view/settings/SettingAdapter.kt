package jp.panta.misskeyandroidclient.view.settings

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import jp.panta.misskeyandroidclient.R
import jp.panta.misskeyandroidclient.databinding.ItemMoveSettingActivityPanelBinding
import jp.panta.misskeyandroidclient.databinding.ItemSettingGroupBinding
import jp.panta.misskeyandroidclient.databinding.ItemSharedCheckboxBinding
import jp.panta.misskeyandroidclient.databinding.ItemSharedSwitchBinding
import jp.panta.misskeyandroidclient.viewmodel.setting.*
import java.lang.IllegalArgumentException


class SettingAdapter(
    val viewLifecycleOwner: LifecycleOwner
) : ListAdapter<Shared, SettingAdapter.SharedHolder>(ItemCallback()){

    class ItemCallback : DiffUtil.ItemCallback<Shared>(){
        override fun areContentsTheSame(oldItem: Shared, newItem: Shared): Boolean {
            return equal(oldItem, newItem)
        }

        override fun areItemsTheSame(oldItem: Shared, newItem: Shared): Boolean {
            return equal(oldItem, newItem)
        }

        private fun equal(newItem: Shared, oldItem: Shared): Boolean{
            if(oldItem.javaClass.name != newItem.javaClass.name){
                return false
            }
            return if(oldItem is SharedItem && newItem is SharedItem){
                oldItem.key == newItem.key && oldItem.titleStringRes == newItem.titleStringRes
            }else{
                false
            }
        }
    }

    abstract class SharedHolder(view: View) : RecyclerView.ViewHolder(view)
    class ItemSharedCheckboxHolder(val binding: ItemSharedCheckboxBinding) : SharedHolder(binding.root)
    class ItemSharedSwitchHolder(val binding: ItemSharedSwitchBinding) : SharedHolder(binding.root)
    class ItemMoveSettingActivityPanelHolder(val binding: ItemMoveSettingActivityPanelBinding) : SharedHolder(binding.root)
    class ItemSettingGroupHolder(val binding: ItemSettingGroupBinding) : SharedHolder(binding.root)

    companion object{
        const val GROUP = 0
        const val CHECK = 1
        const val SWITCH = 2
        const val MOVE = 3
    }

    override fun getItemViewType(position: Int): Int {
        return when(val item = getItem(position)){
            is Group -> GROUP
            is BooleanSharedItem ->{
                when(item.choiceType){
                    BooleanSharedItem.ChoiceType.CHECK_BOX -> CHECK
                    BooleanSharedItem.ChoiceType.SWITCH -> SWITCH
                }
            }
            is MoveSettingActivityPanel<*> ->{
                MOVE
            }
            else -> throw IllegalArgumentException("not found")
        }
    }
    override fun onBindViewHolder(holder: SharedHolder, position: Int) {
        when(holder){
            is ItemSharedSwitchHolder ->{
                holder.binding.item = getItem(position) as BooleanSharedItem
                holder.binding.lifecycleOwner = viewLifecycleOwner
                holder.binding.executePendingBindings()
            }
            is ItemSharedCheckboxHolder ->{
                holder.binding.item = getItem(position) as BooleanSharedItem
                holder.binding.lifecycleOwner = viewLifecycleOwner
                holder.binding.executePendingBindings()
            }
            is ItemSettingGroupHolder ->{
                holder.binding.item = getItem(position) as Group
                val a = SettingAdapter(viewLifecycleOwner)
                a.submitList((getItem(position) as Group).items)
                holder.binding.childItemsView.apply{
                    layoutManager = LinearLayoutManager(holder.binding.root.context)
                    adapter = a

                }
                holder.binding.lifecycleOwner = viewLifecycleOwner
                holder.binding.executePendingBindings()
            }
            is ItemMoveSettingActivityPanelHolder ->{
                holder.binding.item = getItem(position) as MoveSettingActivityPanel<*>
                holder.binding.lifecycleOwner = viewLifecycleOwner
                holder.binding.executePendingBindings()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SharedHolder {
        return when(viewType){
            CHECK ->{
                val binding = makeBinding<ItemSharedCheckboxBinding>(parent, R.layout.item_shared_checkbox)
                ItemSharedCheckboxHolder(binding)
            }
            SWITCH ->{
                val binding = makeBinding<ItemSharedSwitchBinding>(parent, R.layout.item_shared_switch)
                ItemSharedSwitchHolder(binding)
            }
            GROUP ->{
                val binding = makeBinding<ItemSettingGroupBinding>(parent, R.layout.item_setting_group)
                ItemSettingGroupHolder(binding)
            }
            MOVE ->{
                val binding = makeBinding<ItemMoveSettingActivityPanelBinding>(parent, R.layout.item_move_setting_activity_panel)
                ItemMoveSettingActivityPanelHolder(binding)
            }
            else -> throw IllegalArgumentException("not found")

        }
    }

    private fun<T: ViewDataBinding> makeBinding(parent: ViewGroup, @LayoutRes res: Int): T{
        return DataBindingUtil.inflate<T>(LayoutInflater.from(parent.context), res, parent, false)
    }
}
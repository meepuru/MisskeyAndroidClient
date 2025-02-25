package jp.panta.misskeyandroidclient.ui.notification

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wada811.databinding.dataBinding
import jp.panta.misskeyandroidclient.MiApplication
import jp.panta.misskeyandroidclient.R
import jp.panta.misskeyandroidclient.databinding.FragmentNotificationBinding
import net.pantasystem.milktea.model.account.page.Pageable
import jp.panta.misskeyandroidclient.ui.ScrollableTop
import jp.panta.misskeyandroidclient.ui.notes.viewmodel.NotesViewModel
import jp.panta.misskeyandroidclient.ui.notification.viewmodel.NotificationViewData
import jp.panta.misskeyandroidclient.ui.notification.viewmodel.NotificationViewModel
import jp.panta.misskeyandroidclient.ui.notification.viewmodel.NotificationViewModelFactory
import jp.panta.misskeyandroidclient.viewmodel.timeline.CurrentPageableTimelineViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview

@ExperimentalCoroutinesApi
@FlowPreview
class NotificationFragment : Fragment(R.layout.fragment_notification), ScrollableTop {


    lateinit var mLinearLayoutManager: LinearLayoutManager
    lateinit var mViewModel: NotificationViewModel

    private val mBinding: FragmentNotificationBinding by dataBinding()
    val notesViewModel by activityViewModels<NotesViewModel>()

    val currentPageableTimelineViewModel: CurrentPageableTimelineViewModel by activityViewModels()


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mLinearLayoutManager = LinearLayoutManager(requireContext())

        val miApplication = context?.applicationContext as MiApplication


        //val nowConnectionInstance = miApplication.currentConnectionInstanceLiveData.value
        val factory = NotificationViewModelFactory(miApplication)
        mViewModel = ViewModelProvider(this, factory).get(NotificationViewModel::class.java)



        val adapter = NotificationListAdapter(diffUtilItemCallBack, notesViewModel, mViewModel, viewLifecycleOwner)

        mBinding.notificationListView.adapter = adapter
        mBinding.notificationListView.layoutManager = mLinearLayoutManager


        //mViewModel.loadInit()

        mViewModel.notificationsLiveData.observe(viewLifecycleOwner) {
            adapter.submitList(it)
        }

        mViewModel.isLoading.observe(viewLifecycleOwner) {
            mBinding.notificationSwipeRefresh.isRefreshing = it
        }

        mBinding.notificationSwipeRefresh.setOnRefreshListener {
            mViewModel.loadInit()
        }


        mBinding.notificationListView.addOnScrollListener(mScrollListener)


    }


    override fun onResume() {
        super.onResume()

        currentPageableTimelineViewModel.setCurrentPageable(Pageable.Notification())
    }

    @ExperimentalCoroutinesApi
    private val mScrollListener = object : RecyclerView.OnScrollListener(){
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)

            val firstVisibleItemPosition = mLinearLayoutManager.findFirstVisibleItemPosition()
            val endVisibleItemPosition = mLinearLayoutManager.findLastVisibleItemPosition()
            val itemCount = mLinearLayoutManager.itemCount


            if(firstVisibleItemPosition == 0){
                Log.d("", "先頭")
            }

            if(endVisibleItemPosition == (itemCount - 1)){
                Log.d("", "後ろ")
                //mTimelineViewModel?.getOldTimeline()
                mViewModel.loadOld()

            }

        }
    }

    private val diffUtilItemCallBack = object : DiffUtil.ItemCallback<NotificationViewData>(){
        override fun areContentsTheSame(
            oldItem: NotificationViewData,
            newItem: NotificationViewData
        ): Boolean {
            return oldItem == newItem
        }

        override fun areItemsTheSame(
            oldItem: NotificationViewData,
            newItem: NotificationViewData
        ): Boolean {
            return oldItem.id == newItem.id
        }
    }

    override fun showTop() {
        mLinearLayoutManager.scrollToPosition(0)
    }
}
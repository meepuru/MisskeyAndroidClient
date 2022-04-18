package jp.panta.misskeyandroidclient.ui.notification

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.lifecycle.lifecycleScope
import com.wada811.databinding.dataBinding
import jp.panta.misskeyandroidclient.MiApplication
import jp.panta.misskeyandroidclient.R
import jp.panta.misskeyandroidclient.databinding.FragmentNotificationMentionBinding
import net.pantasystem.milktea.model.account.page.Page
import net.pantasystem.milktea.model.account.page.PageType
import jp.panta.misskeyandroidclient.ui.PageableFragmentFactory
import jp.panta.misskeyandroidclient.ui.settings.page.PageTypeNameMap
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import net.pantasystem.milktea.model.account.page.PageableTemplate

@FlowPreview
@ExperimentalCoroutinesApi
class NotificationMentionFragment : Fragment(R.layout.fragment_notification_mention){

    private val mBinding: FragmentNotificationMentionBinding by dataBinding()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val pageableTypeNameMap = PageTypeNameMap(view.context)
        val pagerItems = listOf(
            net.pantasystem.milktea.model.account.page.PageableTemplate(null)
                .notification(pageableTypeNameMap.get(net.pantasystem.milktea.model.account.page.PageType.NOTIFICATION)),
            net.pantasystem.milktea.model.account.page.PageableTemplate(null)
                .mention(pageableTypeNameMap.get(net.pantasystem.milktea.model.account.page.PageType.MENTION))
        )

        val notificationPagerAdapter =  PagerAdapter(pagerItems)

        mBinding.notificationPager.adapter = notificationPagerAdapter
        mBinding.notificationTab.setupWithViewPager(mBinding.notificationPager)

        val miCore = requireContext().applicationContext as MiApplication
        miCore.getAccountStore().observeCurrentAccount.filterNotNull().onEach {
            notificationPagerAdapter.notifyDataSetChanged()
        }.launchIn(lifecycleScope)

    }

    inner class PagerAdapter(val pages: List<net.pantasystem.milktea.model.account.page.Page>) : FragmentStatePagerAdapter(childFragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT){

        private fun createFragment(position: Int): Fragment {
            return PageableFragmentFactory.create(pages[position])
        }

        override fun getPageTitle(position: Int): CharSequence {
            return pages[position].title
        }

        override fun getItem(position: Int): Fragment {
            return createFragment(position)
        }

        override fun getCount(): Int {
            return pages.size
        }



    }
}
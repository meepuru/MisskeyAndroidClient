package jp.panta.misskeyandroidclient.ui.notes.view

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.databinding.BindingAdapter
import jp.panta.misskeyandroidclient.R
import jp.panta.misskeyandroidclient.mfm.MFMDecorator
import jp.panta.misskeyandroidclient.mfm.MFMParser
import net.pantasystem.milktea.model.emoji.Emoji
import net.pantasystem.milktea.model.notes.Translation
import net.pantasystem.milktea.common.State
import net.pantasystem.milktea.common.StateContent

object TranslationHelper {


    @JvmStatic
    @BindingAdapter("translationState", "emojis")
    fun TextView.setTranslatedText(state: State<Translation>?, emojis: List<Emoji>?) {
        if(state == null) {
            this.visibility = View.GONE
            return
        }

        if(state is State.Loading) {
            this.visibility = View.GONE
            return
        }

        val translation = runCatching {
            (state.content as StateContent.Exist).rawContent
        }.getOrNull()
        this.visibility = View.VISIBLE

        if(state is State.Error) {
            this.text = context.getString(R.string.error_s, state.throwable.toString())
        }
        if(translation == null) {
            return
        }

        val text = context.getString(R.string.translated_from_s, translation.sourceLang) + translation.text
        val root = MFMParser.parse(text, emojis)!!
        this.text = MFMDecorator.decorate(this, root)

    }

    @JvmStatic
    @BindingAdapter("translationState")
    fun ViewGroup.translationVisibility(state: State<Translation>?) {
        if(state == null) {
            this.visibility = View.GONE
            return
        }
        this.visibility = if(state.content is StateContent.NotExist && state is State.Fixed) {
            View.GONE
        }else{
            View.VISIBLE
        }
    }
}
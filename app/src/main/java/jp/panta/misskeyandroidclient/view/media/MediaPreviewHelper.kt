package jp.panta.misskeyandroidclient.view.media

import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import jp.panta.misskeyandroidclient.R
import jp.panta.misskeyandroidclient.model.drive.FileProperty
import jp.panta.misskeyandroidclient.viewmodel.media.FileViewData
import jp.panta.misskeyandroidclient.viewmodel.media.MediaViewData
import java.lang.IndexOutOfBoundsException

object MediaPreviewHelper{

    @BindingAdapter(
        "thumbnailTopLeft",
        "thumbnailTopRight",
        "thumbnailBottomLeft",
        "thumbnailBottomRight",
        "actionButtonTopLeft",
        "actionButtonTopRight",
        "actionButtonBottomLeft",
        "actionButtonBottomRight",
        "baseFrameTopLeft",
        "baseFrameTopRight",
        "baseFrameBottomLeft",
        "baseFrameBottomRight",
        "shieldLeft",
        "shieldRight",
        "previewMediaFiles")
    @JvmStatic
    fun ViewGroup.setMediaPreview(
        thumbnailTopLeft: ImageView,
        thumbnailTopRight: ImageView,
        thumbnailBottomLeft: ImageView,
        thumbnailBottomRight: ImageView,
        actionButtonTopLeft: ImageButton,
        actionButtonTopRight: ImageButton,
        actionButtonBottomLeft: ImageButton,
        actionButtonBottomRight: ImageButton,
        baseFrameTopLeft: FrameLayout,
        baseFrameTopRight: FrameLayout,
        baseFrameBottomLeft: FrameLayout,
        baseFrameBottomRight: FrameLayout,
        shieldLeft: LinearLayout,
        shieldRight: LinearLayout,
        previewMediaFiles: List<FileProperty>?
    ){
        val thumbnailViews = listOf(thumbnailTopLeft, thumbnailTopRight, thumbnailBottomLeft, thumbnailBottomRight)
        val actionButtons = listOf(actionButtonTopLeft, actionButtonTopRight, actionButtonBottomLeft, actionButtonBottomRight)
        val frames = listOf(baseFrameTopLeft, baseFrameTopRight, baseFrameBottomLeft, baseFrameBottomRight)
        if(previewMediaFiles.isNullOrEmpty()){
            Log.d("MediaPreviewHelper", "previewMediaFiles is null")
            for(n in 0.until(4)){
                thumbnailViews[n].visibility = View.GONE
                actionButtons[n].visibility = View.GONE
                frames[n].visibility = View.GONE
            }
            return
        }


        for(n in 0.until(4)){
            val thumbnail = thumbnailViews[n]
            val actionButton = actionButtons[n]
            val frame = frames[n]
            try{
                val file = previewMediaFiles[n]
                setPreview(thumbnail, actionButton, frame, file)
                if(n == 0){
                    Log.d("", "左、表示")
                    shieldLeft.visibility = View.VISIBLE
                }else if(n == 1){
                    Log.d("", "右、表示")
                    shieldRight.visibility = View.VISIBLE
                }

            }catch(e: IndexOutOfBoundsException){
                if(n == 0){
                    Log.d("", "左、非表示")
                    shieldLeft.visibility = View.GONE
                }else if(n == 1){
                    Log.d("", "右、非表示")
                    shieldRight.visibility = View.GONE
                }
                frame.visibility = View.GONE
                thumbnail.visibility = View.GONE
                actionButton.visibility = View.GONE

            }
        }
    }

    fun setPreview(thumbnail: ImageView, actionButton: ImageButton, frame: FrameLayout, file: FileProperty){
        //VISIBLEにしかしない
        frame.visibility =View.VISIBLE
        thumbnail.visibility = View.VISIBLE
        if(file.type?.contains("audio") == true){
            Glide.with(thumbnail)
                .load(R.drawable.ic_music_note_black_24dp)
                .centerCrop()
                .error(R.drawable.ic_cloud_off_black_24dp)
                .into(thumbnail)
        }else{
            Glide.with(thumbnail)
                .load(file.thumbnailUrl)
                .centerCrop()
                .error(R.drawable.ic_cloud_off_black_24dp)
                .into(thumbnail)
        }

        if(file.type?.contains("video") == true){
            actionButton.visibility = View.VISIBLE
            Glide.with(actionButton)
                .load(R.drawable.ic_play_circle_outline_black_24dp)
                .centerCrop()
                .into(actionButton)
        }else{
            actionButton.visibility = View.INVISIBLE

        }



    }

    @BindingAdapter("thumbnailView", "nsfwMessage", "playButton", "mediaViewData", "fileIndex")
    @JvmStatic
    fun FrameLayout.setPreview(thumbnailView: ImageView, nsfwMessage: TextView, playButton: ImageButton, mediaViewData: MediaViewData, fileIndex: Int){
        try{
            val file = mediaViewData.files[fileIndex]
            this.visibility = View.VISIBLE

            Log.d("MediaPreviewHelper", "type: ${file.type}, url:${file.thumbnailUrl}")
            when(file.type){
                FileViewData.Type.IMAGE, FileViewData.Type.VIDEO -> {
                    Glide.with(thumbnailView)
                        .load(file.thumbnailUrl)
                        .centerCrop()
                        .into(thumbnailView)

                    when(file.type){
                        FileViewData.Type.IMAGE ->{
                            playButton.visibility = View.GONE
                        }
                        else ->{
                            playButton.visibility = View.VISIBLE
                            Glide.with(playButton)
                                .load(R.drawable.ic_play_circle_outline_black_24dp)
                                .centerInside()
                                .into(playButton)
                        }
                    }
                    //thumbnailView.visibility = View.VISIBLE

                }
                FileViewData.Type.SOUND -> {
                    playButton.visibility = View.VISIBLE
                    Glide.with(playButton.context)
                        .load(R.drawable.ic_music_note_black_24dp)
                        .centerInside()
                        .into(playButton)
                }
                else ->{
                    this.visibility = View.GONE
                }
            }

            /*val isHiding = file.isHiding.value?: false
            if(isHiding){
                thumbnailView.visibility = View.GONE
                nsfwMessage.visibility = View.VISIBLE
            }else{
                thumbnailView.visibility = View.VISIBLE
                nsfwMessage.visibility = View.GONE
            }*/
        }catch(e: IndexOutOfBoundsException){
            this.visibility = View.GONE
        }
    }

    @BindingAdapter("leftMediaBase", "rightMediaBase", "mediaViewData")
    @JvmStatic
    fun LinearLayout.visibilityControl(leftMediaBase: LinearLayout, rightMediaBase: LinearLayout, mediaViewData: MediaViewData){
        when {
            mediaViewData.files.isEmpty() -> {
                leftMediaBase.visibility = View.GONE
                rightMediaBase.visibility = View.GONE
            }
            mediaViewData.files.size < 2 ->{
                leftMediaBase.visibility = View.VISIBLE
                rightMediaBase.visibility = View.GONE
            }
            else -> {
                leftMediaBase.visibility = View.VISIBLE
                rightMediaBase.visibility = View.VISIBLE
            }
        }
    }



}
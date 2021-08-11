package jp.panta.misskeyandroidclient.ui.notes

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.rememberImagePainter
import jp.panta.misskeyandroidclient.model.notes.NoteCaptureAPIAdapter
import jp.panta.misskeyandroidclient.model.notes.NoteRelation
import jp.panta.misskeyandroidclient.ui.components.CustomEmojiText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.withContext

@ExperimentalCoroutinesApi
@Composable
fun ItemRenoteUser(
    note: NoteRelation,
    onClick: ()->Unit,
    noteCaptureAPIAdapter: NoteCaptureAPIAdapter?,
    isUserNameDefault: Boolean = false
) {

    LaunchedEffect(key1 = true,){
        withContext(Dispatchers.IO) {
            noteCaptureAPIAdapter?.capture(note.note.id)?.launchIn(this)
        }
    }

    Card(
        shape = RoundedCornerShape(0.dp),
        modifier = Modifier.padding(0.5.dp).clickable {
            onClick.invoke()
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = rememberImagePainter(note.user.avatarUrl),
                contentDescription = null,
                modifier = Modifier.size(48.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                if(isUserNameDefault){
                    CustomEmojiText(text = note.user.getDisplayUserName(), emojis = note.user.emojis)
                }else{
                    CustomEmojiText(text = note.user.getDisplayName(), emojis = note.user.emojis)
                }
                if(isUserNameDefault){
                    Text(text = note.user.getDisplayName())
                }else{
                    Text(text = note.user.getDisplayUserName())
                }
            }

        }
    }
}



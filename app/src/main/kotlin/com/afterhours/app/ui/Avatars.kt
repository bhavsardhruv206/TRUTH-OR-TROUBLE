package com.afterhours.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.afterhours.app.Avatar

/** Profile pictures by player name, for the game and results screens. */
val LocalAvatars = staticCompositionLocalOf<Map<String, Avatar>> { emptyMap() }

/** The fun avatars that come with the app. */
val avatarChoices = listOf(
    "😎", "🤠", "🥳", "😈", "👻", "🤖", "👽", "🤡",
    "🦊", "🐯", "🐼", "🐸", "🦄", "🐙", "🦁", "🐵",
)

/** A round profile picture: a bundled avatar, or a person (heart for partners) when none is set. */
@Composable
fun AvatarBadge(
    avatar: Avatar?,
    size: Dp,
    modifier: Modifier = Modifier,
    partner: Boolean = false,
    highlighted: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val base = modifier.size(size).clip(CircleShape)
        .background(GraphiteHigh)
        .border(1.dp, if (highlighted) Champagne else HairlineStrong, CircleShape)
        .let { if (onClick != null) it.clickable(onClick = onClick) else it }
    Box(base, contentAlignment = Alignment.Center) {
        when (avatar) {
            is Avatar.Emoji -> Text(avatar.char, fontSize = (size.value * .55f).sp)
            null -> Icon(
                if (partner) Icons.Filled.Favorite else Icons.Filled.Person, null,
                Modifier.size(size * .55f), tint = if (highlighted) Champagne else Stone,
            )
        }
    }
}

/** Choose a profile picture from the avatars that come with the app. */
@Composable
fun AvatarPicker(
    current: Avatar?,
    onPick: (Avatar?) -> Unit,
    onDismiss: () -> Unit,
) {
    val S = LocalStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Graphite,
        title = { Text(S.profilePicture, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column {
                avatarChoices.chunked(4).forEach { row ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                        row.forEach { e ->
                            val selected = (current as? Avatar.Emoji)?.char == e
                            AvatarBadge(Avatar.Emoji(e), 52.dp, highlighted = selected) { onPick(Avatar.Emoji(e)) }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onDismiss) { Text(S.done, color = Champagne) } },
        dismissButton = if (current != null) { { TextButton({ onPick(null) }) { Text(S.remove, color = Stone) } } } else null,
    )
}

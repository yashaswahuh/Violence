package com.music.violence.share

import com.music.violence.data.YtMusicRepository
import com.music.violence.data.model.SearchFilter
import com.music.violence.data.model.SearchResult
import com.music.violence.data.model.Song
import com.music.violence.data.share.SongRef
import com.music.violence.data.share.SongSource

/**
 * Turns a song named by a shared link into a playable [Song].
 *
 * This is the only network use of receiving a song, and it runs only after the user chose
 * "Play now", "Add to queue" or "Add to playlist". A link with a YouTube id is looked up by that
 * id (the title in the link is never used for it); a link with only words, or a Spotify id,
 * searches for "title artist" and takes the first song result.
 */
object SharedSongResolver {

    suspend fun resolve(ref: SongRef): Song? =
        if (ref.source == SongSource.YOUTUBE) {
            YtMusicRepository.trackLinks(ref.id).getOrNull()
        } else {
            YtMusicRepository.search(ref.searchWords(), SearchFilter.SONGS).getOrNull()
                ?.filterIsInstance<SearchResult.Track>()?.firstOrNull()?.song
        }
}

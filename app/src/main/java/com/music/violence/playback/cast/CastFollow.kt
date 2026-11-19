package com.music.violence.playback.cast

import kotlin.math.abs

/**
 * Which row of the phone's queue the receiver has moved to, when it has moved on by itself.
 *
 * Pure, so it can be tested: the receiver's status is a Cast framework object that cannot be built
 * outside a device. [CastPlayback] reads what it can from the status and asks this.
 *
 * The receiver does not always say which track it is on. A queued item other than the playing one often
 * reaches the phone without its media, so the queue cannot be read back for its id; the app then stayed on
 * the song before while the speaker played the next one. When the track is not named, the position in the
 * receiver's queue stands in for it, since the phone keeps its own queue in the same order.
 */
internal object CastFollow {

    sealed interface Outcome {
        /** Already on it, or nothing here to follow: leave the phone's player alone. */
        data object Stay : Outcome

        /** Move the phone's player to this row. */
        data class MoveTo(val index: Int) : Outcome

        /** Cannot tell yet: do not record the change, so a later status is looked at again. */
        data object Unknown : Outcome
    }

    const val NO_ITEM = -1

    /**
     * @param mediaId the receiver's new track, when it said which.
     * @param localMediaIds the phone's queue, in order, by media id.
     * @param localIndex the row the phone's player is on.
     * @param previousItemId the receiver's item before this change, [NO_ITEM] on the first status.
     * @param itemId the receiver's item now.
     * @param receiverOrder the receiver's queue, in order, by item id.
     */
    fun decide(
        mediaId: String?,
        localMediaIds: List<String>,
        localIndex: Int,
        previousItemId: Int,
        itemId: Int,
        receiverOrder: List<Int>,
    ): Outcome {
        if (mediaId != null) {
            if (localMediaIds.getOrNull(localIndex) == mediaId) return Outcome.Stay
            // A track queued twice is the case where more than one row answers: the near one is meant.
            val row = localMediaIds.indices
                .filter { localMediaIds[it] == mediaId }
                .minByOrNull { abs(it - localIndex) }
            return if (row != null) Outcome.MoveTo(row) else Outcome.Stay
        }
        // Nothing before it to measure from: the first status of a session names no change.
        if (previousItemId == NO_ITEM) return Outcome.Stay
        val from = receiverOrder.indexOf(previousItemId)
        val to = receiverOrder.indexOf(itemId)
        if (from < 0 || to <= from) return Outcome.Unknown
        val row = localIndex + (to - from)
        return if (row in localMediaIds.indices) Outcome.MoveTo(row) else Outcome.Unknown
    }
}

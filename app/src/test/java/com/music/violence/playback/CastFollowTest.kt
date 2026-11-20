package com.music.violence.playback

import com.music.violence.playback.cast.CastFollow
import com.music.violence.playback.cast.CastFollow.Outcome
import com.music.violence.playback.cast.RemoteSpeaker
import com.music.violence.playback.cast.Speakers
import com.music.violence.playback.cast.NoSpeaker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** How the app follows a receiver that moved on to the next track by itself. */
class CastFollowTest {

    private val queue = listOf("a", "b", "c", "d", "e")
    private val receiverOrder = listOf(10, 11, 12, 13)

    @Test fun namedTrackIsFollowedToItsRow() {
        val outcome = CastFollow.decide("c", queue, localIndex = 1, previousItemId = 11, itemId = 12, receiverOrder = receiverOrder)
        assertEquals(Outcome.MoveTo(2), outcome)
    }

    @Test fun namedTrackAlreadyCurrentChangesNothing() {
        val outcome = CastFollow.decide("b", queue, localIndex = 1, previousItemId = 10, itemId = 11, receiverOrder = receiverOrder)
        assertEquals(Outcome.Stay, outcome)
    }

    @Test fun trackQueuedTwiceTakesTheNearerRow() {
        val twice = listOf("a", "x", "b", "x", "c")
        assertEquals(Outcome.MoveTo(3), CastFollow.decide("x", twice, 4, 10, 11, receiverOrder))
        assertEquals(Outcome.MoveTo(1), CastFollow.decide("x", twice, 0, 10, 11, receiverOrder))
    }

    @Test fun unnamedTrackIsFoundByItsPlaceInTheReceiversQueue() {
        // The receiver went from item 11 to item 12 without saying what 12 is: one row on from the phone's.
        val outcome = CastFollow.decide(null, queue, localIndex = 1, previousItemId = 11, itemId = 12, receiverOrder = receiverOrder)
        assertEquals(Outcome.MoveTo(2), outcome)
    }

    @Test fun unnamedTrackTwoAheadMovesTwoRows() {
        val outcome = CastFollow.decide(null, queue, localIndex = 0, previousItemId = 10, itemId = 12, receiverOrder = receiverOrder)
        assertEquals(Outcome.MoveTo(2), outcome)
    }

    @Test fun anUnreadableChangeIsLeftToBeLookedAtAgain() {
        // The item before it is not in the list, or the new one is not after it.
        assertEquals(Outcome.Unknown, CastFollow.decide(null, queue, 1, 99, 12, receiverOrder))
        assertEquals(Outcome.Unknown, CastFollow.decide(null, queue, 1, 12, 11, receiverOrder))
        // And it would land past the end of the phone's queue.
        assertEquals(Outcome.Unknown, CastFollow.decide(null, queue, 4, 10, 11, receiverOrder))
    }

    @Test fun theFirstStatusOfASessionNamesNoChange() {
        assertEquals(Outcome.Stay, CastFollow.decide(null, queue, 1, CastFollow.NO_ITEM, 10, receiverOrder))
    }

    @Test fun aTrackThePhoneDoesNotHoldIsNotFollowed() {
        assertEquals(Outcome.Stay, CastFollow.decide("zzz", queue, 1, 10, 11, receiverOrder))
    }

    @Test fun speakersReportAnAdvanceOnceWhicheverReceiverMadeIt() {
        val cast = object : RemoteSpeaker by NoSpeaker {
            var advanced = true
            override fun takeAdvancedOnItsOwn(): Boolean = advanced.also { advanced = false }
        }
        val speakers = Speakers(listOf(cast, NoSpeaker))
        assertTrue(speakers.takeAdvancedOnItsOwn())
        assertFalse(speakers.takeAdvancedOnItsOwn())
    }
}

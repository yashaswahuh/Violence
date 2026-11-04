package com.music.violence.data.share

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IncomingLinkGateTest {

    @Test
    fun theFirstSightingIsAdmitted() {
        assertTrue(IncomingLinkGate().admit("song:yt:a", 0))
    }

    @Test
    fun theSameLinkAgainIsIgnoredAndEachSightingExtendsTheWindow() {
        val gate = IncomingLinkGate()
        assertTrue(gate.admit("k", 0))
        assertFalse(gate.admit("k", 1_000))
        assertFalse(gate.admit("k", 9_000))
        assertFalse(gate.admit("k", 17_000), "still touching: seen 8 s ago, so still the same contact")
        assertTrue(gate.admit("k", 28_000), "quiet for more than 10 s: a fresh tap")
    }

    @Test
    fun differentLinksAreIndependent() {
        val gate = IncomingLinkGate()
        assertTrue(gate.admit("a", 0))
        assertTrue(gate.admit("b", 10))
        assertFalse(gate.admit("a", 20))
    }

    @Test
    fun atMostFiveDifferentLinksPerWindow() {
        val gate = IncomingLinkGate()
        repeat(5) { assertTrue(gate.admit("k$it", it * 100L)) }
        assertFalse(gate.admit("k5", 600))
        assertFalse(gate.admit("k6", 700))
        assertTrue(gate.admit("k7", 10_700), "the window has passed")
    }

    @Test
    fun forgettingLetsTheUserTapAgainAtOnce() {
        val gate = IncomingLinkGate()
        assertTrue(gate.admit("k", 0))
        assertFalse(gate.admit("k", 500))
        gate.forget("k")
        assertTrue(gate.admit("k", 600))
    }
}

package com.music.violence.data.discord

/**
 * Violence's own Discord application, shared by the phone and desktop presence.
 *
 * Discord reads more off this id than the code suggests. The local socket on
 * the desktop titles the activity with the application's registered name, some
 * Discord surfaces do the same for a gateway activity that carries it, and an
 * activity with no `large_image` shows the application's icon. Both come from
 * the developer portal entry, so the name there has to stay "Violence".
 *
 * It replaced Metrolist's id, inherited along with the Kizzy port, which made
 * presences read "Listening to Metrolist" wherever Discord preferred the
 * application's name to the activity's.
 */
object DiscordApplication {
    const val ID = "1566782172302413885"

    /**
     * Violence's logo, drawn on the card when a track has no artwork of its own
     * and as the small badge beside artwork. Independent of the developer-portal
     * icon, which still has to be replaced there to change what Discord shows
     * next to the application name.
     */
    const val LOGO_URL = "https://raw.githubusercontent.com/yashaswahuh/Violence/main/Logo.png"

    /** Discord refuses a button label longer than this. */
    const val BUTTON_LABEL_MAX = 32
}

# Violence for Wear OS

A watch remote for the Violence phone app, in the Pixel Watch's Material 3
Expressive layout. Now playing sits over the cover, only lightly blurred so the
picture stays readable, with every control in tones taken from the song: a
scalloped "cookie" play button (a circle when paused) with a wavy progress ring
round it that ripples while music plays, tall previous and next pills, and
glass Output (with the crown-volume badge) and More pills. Hold the play button
to scrub; the crown or bezel is the phone's volume. Up Next, Shuffle, Repeat and
Love are in More; the Output picker covers the phone's outputs and **this
watch**. The always-on screen is black and outline-led (about 9% of pixels
lit): a big time, the V logo inside a cookie outline in the song's accent, the
title, artist and elapsed time, and a hairline ring with a wavy arc for
progress. Lists use `TransformingLazyColumn` and M3 buttons; the connection
screens use `EdgeButton`.

The accent engine (`ui/Accent.kt`) picks the most colourful of the four colours
the phone sends and builds the palette from that one hue as OKLCH tones, so the
dark glyph on a pastel button always has the same contrast. A grey cover falls
back to Violence violet. The soft cover is made once per track on the watch
(`PhoneLink.blurred`, a 160 px copy with one small box blur, drawn scaled up),
so it needs no GPU blur and works on every Wear OS version this app supports.
Mock-ups: `fork-docs/wearos-mockups/`.

The watch is never a second player with a queue of its own. As the speaker it
is a receiver, exactly like a Cast device: the phone keeps the queue (paused)
and decides every track, the watch plays the one it is told plus the next two,
reports back, and the phone's app, notification, widgets and the watch UI all
show the same state. Audio is read through the phone's own resolver and cache
over a Wearable channel, so the watch needs no network or account. Tracks the
phone serves as a DASH manifest can't play on the watch yet.

The APK is about 4.5 MB (most of it Media3, for the speaker), R8 is on for every
build type, and the app listens only while it is on screen or playing.

| Path | What it is |
| --- | --- |
| `protocol/.../WearProtocol.kt` | Paths, keys and commands. Compiled into both `:app` and `:wearos` |
| `src/main/java/.../data/PhoneLink.kt` | The watch's view of the phone: Data Layer state in, messages out |
| `src/main/java/.../ui/**` | Now Playing, Up Next, More, connection screens |
| `src/main/java/.../playback/**` | The watch as the speaker: `Speaker`, its player service, the phone-backed data source |
| `../app/src/main/java/.../wear/WearBridge.kt` | Phone side: publishes state, runs the watch's commands, `WatchSpeaker`, the stream server |
| `../app/src/main/java/.../playback/cast/RemoteSpeaker.kt` | The receiver seam Cast and the watch share |
| `../tv/src/main/java/.../wear/WearBridge.kt` | No-op stand-in, so the TV build carries none of it |

## Dev and prod

The Wearable Data Layer only connects apps that have the same package name and
the same signing key. So the watch app takes the phone app's ids:

| Watch build | Package | Pairs with |
| --- | --- | --- |
| `devDebug` | `com.yashaswahuh.violencemusic.dev` | the phone's `devDebug` (debug key) |
| `prodRelease` | `com.yashaswahuh.violencemusic` | the phone's `prodRelease` (release key) |

A watch build only finds a phone build of the same flavor **and** signed with
the same key. Published phone APKs are signed in CI with the key from
`secrets.KEYSTORE_BASE64`, which is not the keystore in a local checkout, so
the prod watch APK for those phones has to come from CI too. `android.yml`
uploads it as `violence-wear-prod-release-signed`.

## Build and install

```bash
./gradlew :wearos:assembleDevDebug :wearos:assembleProdRelease
```

APKs land in `wearos/build/outputs/apk/<flavor>/<type>/ViolenceWear-*.apk`.
To install on a Galaxy Watch, turn on Developer options → ADB debugging and
Wireless debugging on the watch, then:

```bash
adb pair <watch-ip>:<pairing-port>
adb connect <watch-ip>:<port>
adb -s <watch-ip>:<port> install -r wearos/build/outputs/apk/dev/debug/ViolenceWear-dev-debug.apk
```

The phone needs a Violence build that includes `WearBridge` (anything from this
change on), from the same flavor and signed with the same key.

Debug builds are shrunk and non-debuggable, because an un-R8'd, debuggable
Compose app is noticeably slower on a watch CPU. To get a debugger back, pass
`-Pviolence.wearDebuggable=true`.

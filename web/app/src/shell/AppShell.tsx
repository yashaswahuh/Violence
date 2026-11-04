import { lazy, Suspense, useEffect, useState } from 'react';
import { resumeSession, useParty } from '@/party/listenTogether';
import { startPartySync } from '@/party/partySync';
import { useDiscordPresence } from '@/playback/discordPresence';
import { startHistoryReporting } from '@/playback/historyReporting';
import { startLivePresence } from '@/playback/livePresence';
import { Outlet } from 'react-router';
import { PrefetchLink } from '@/components/PrefetchLink';
import { config } from '@/config';
import { useExtension } from '@/extension/useExtension';
import { isMobileBrowser } from '@/lib/platform';
import { useProfile } from '@/profile/store';
import { partyPlayer } from '@/state/player';
import { useUi } from '@/state/ui';
import { useKeyboardShortcuts } from './useKeyboardShortcuts';
import { Banner, bannerButtonClass } from './Banner';
import { MobileTopBar } from './MobileTopBar';
import { PlayerBar } from './PlayerBar';
import { SidePanel } from './SidePanel';
import { Sidebar } from './Sidebar';
import { ViolenceBackground } from '@/components/style/ViolenceBackground';

const mobile = isMobileBrowser();

// It carries am-lyrics; loaded the first time it opens, as the side panel's lyrics are.
const FullScreenPlayer = lazy(() => import('./FullScreenPlayer').then((m) => ({ default: m.FullScreenPlayer })));

/**
 * The app frame (report §1): glass sidebar, the scrolling page, the floating
 * player pill, the side panel, and the bottom band when there is one.
 */
export function AppShell() {
  useKeyboardShortcuts();
  useDiscordPresence();
  // Listen Together: pick a party back up after a reload, and keep the player in step with it.
  useEffect(() => {
    resumeSession();
    return startPartySync();
  }, []);
  // Plays into the account's YouTube Music history (Settings › Share Listening Activity).
  useEffect(() => startHistoryReporting(), []);
  // Counts this tab in the "listening now" figure beside the phone and PC apps.
  useEffect(() => startLivePresence(), []);
  const extension = useExtension();
  const bannerDismissed = useUi((s) => s.installBannerDismissed);
  const dismissBanner = useUi((s) => s.dismissInstallBanner);
  const fullScreen = useUi((s) => s.fullScreen);
  // The one-time, skippable offer to make a profile; shown only when nothing more important is.
  const hasProfile = useProfile((s) => s.profile !== null);
  const [suggestProfile, setSuggestProfile] = useState(() => useProfile.getState().shouldSuggest());
  const handleSuggestion = () => {
    useProfile.getState().markSuggestionHandled();
    setSuggestProfile(false);
  };

  // Listen Together notices that matter wherever the listener is in the app.
  const partyAudioBlocked = useParty((s) => !!s.code && s.audioBlocked);
  const partyStalled = useParty((s) => (s.code ? s.stalledTrack : null));

  let banner = null;
  if (partyAudioBlocked) {
    banner = (
      <Banner
        title="Tap to start the party’s audio."
        detail="Your browser holds back sound until you interact with the page."
        action={
          <button className={bannerButtonClass} onClick={() => partyPlayer.play()}>
            Start audio
          </button>
        }
      />
    );
  } else if (partyStalled) {
    banner = (
      <Banner
        title="Can’t play this song here."
        detail={`“${partyStalled}” won’t play in this browser. You’ll rejoin the party on the next song.`}
        action={
          <PrefetchLink className={bannerButtonClass} to="/settings/listen-together">
            Open party
          </PrefetchLink>
        }
      />
    );
  } else if (mobile) {
    banner = (
      <Banner
        title="Violence is better in the app."
        detail="Phone browsers can’t run the Violence extension. The Android app has everything."
        action={
          config.androidAppUrl ? (
            <a className={bannerButtonClass} href={config.androidAppUrl} target="_blank" rel="noreferrer">
              Get the App
            </a>
          ) : undefined
        }
      />
    );
  } else if (extension.state === 'missing' && !bannerDismissed) {
    banner = (
      <Banner
        title="Play YouTube Music with the Violence extension."
        detail="Playback, YouTube Music sign-in, Spotify and Discord need the extension. Addons and lyrics work without it."
        action={
          <PrefetchLink className={bannerButtonClass} to="/extension">
            Get the Extension
          </PrefetchLink>
        }
        onDismiss={dismissBanner}
      />
    );
  } else if (suggestProfile && !hasProfile) {
    banner = (
      <Banner
        title="Make your Violence profile."
        detail="Pick a name and a picture so friends see you in Listen Together. Optional: everything works without it."
        action={
          <PrefetchLink className={bannerButtonClass} to="/settings/profile" onClick={handleSuggestion}>
            Create Profile
          </PrefetchLink>
        }
        onDismiss={handleSuggestion}
      />
    );
  }

  return (
    <div className="grid h-full grid-rows-[minmax(0,1fr)_auto]">
      <ViolenceBackground />
      {/* The side panel slides in from beyond the right edge; never let it scroll the page. */}
      <div className="relative min-h-0 overflow-hidden">
        <Sidebar />
        <MobileTopBar />
        {/* The page scrolls under the fixed sidebar and the floating player. */}
        <main
          id="scrollable-page"
          className="absolute inset-0 overflow-x-hidden overflow-y-auto pl-(--web-navigation-width) [scrollbar-gutter:stable] data-tint:bg-(--page-tint) data-tint:transition-[background-color] data-tint:duration-300 data-tint:ease-in max-sm:pt-(--mobile-top-bar-height) max-sm:pl-0"
        >
          {/* Clears the floating pill: its height, its offset, and some air. */}
          <div className="min-h-full pb-[calc(var(--player-pill-height)+var(--player-bottom-offset)+40px)]">
            <Outlet />
          </div>
        </main>
        <PlayerBar />
        <SidePanel />
      </div>
      {banner}
      {fullScreen && (
        <Suspense fallback={null}>
          <FullScreenPlayer />
        </Suspense>
      )}
    </div>
  );
}

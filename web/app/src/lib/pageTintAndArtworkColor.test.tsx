// @vitest-environment happy-dom
import { act } from 'react';
import { createRoot } from 'react-dom/client';
import { beforeEach, afterEach, describe, expect, it } from 'vitest';
import { usePageTint } from './usePageTint';
import { useViolenceStyle } from '@/state/violenceStyle';
import type { ArtworkColor } from './artworkColor';

describe('usePageTint', () => {
  let container: HTMLDivElement;
  let pageElement: HTMLDivElement;

  beforeEach(() => {
    container = document.createElement('div');
    document.body.appendChild(container);

    pageElement = document.createElement('div');
    pageElement.id = 'scrollable-page';
    document.body.appendChild(pageElement);

    useViolenceStyle.getState().setEnabled(false);
  });

  afterEach(() => {
    if (container.parentElement) {
      document.body.removeChild(container);
    }
    if (pageElement.parentElement) {
      document.body.removeChild(pageElement);
    }
  });

  function renderHook(color: ArtworkColor | null) {
    const root = createRoot(container);
    function TestComponent({ colorProp }: { colorProp: ArtworkColor | null }) {
      usePageTint(colorProp);
      return null;
    }
    act(() => {
      root.render(<TestComponent colorProp={color} />);
    });
    return {
      update: (nextColor: ArtworkColor | null) => {
        act(() => {
          root.render(<TestComponent colorProp={nextColor} />);
        });
      },
      unmount: () => {
        act(() => {
          root.unmount();
        });
      },
    };
  }

  it('sets --page-tint and dataset.tint when Violence Style is disabled', () => {
    useViolenceStyle.getState().setEnabled(false);
    const color: ArtworkColor = { background: 'rgb(200, 100, 150)', dark: true };
    const { unmount } = renderHook(color);

    expect(pageElement.style.getPropertyValue('--page-tint')).toBe('rgb(200, 100, 150)');
    expect(pageElement.dataset.tint).toBe('dark');

    unmount();
    expect(pageElement.style.getPropertyValue('--page-tint')).toBe('');
    expect(pageElement.dataset.tint).toBeUndefined();
  });

  it('never sets --page-tint or dataset.tint when Violence Style is enabled', () => {
    useViolenceStyle.getState().setEnabled(true);
    const color: ArtworkColor = { background: 'rgb(255, 105, 180)', dark: false };
    const { unmount } = renderHook(color);

    // In Violence Style, background is the live song glow, so page tint must never be applied
    expect(pageElement.style.getPropertyValue('--page-tint')).toBe('');
    expect(pageElement.dataset.tint).toBeUndefined();

    unmount();
  });

  it('removes lingering page tint when Violence Style is enabled', () => {
    // Simulate lingering tint from a previous page
    pageElement.style.setProperty('--page-tint', 'rgb(255, 105, 180)');
    pageElement.dataset.tint = 'dark';

    useViolenceStyle.getState().setEnabled(true);
    const color: ArtworkColor = { background: 'rgb(255, 105, 180)', dark: true };
    const { unmount } = renderHook(color);

    expect(pageElement.style.getPropertyValue('--page-tint')).toBe('');
    expect(pageElement.dataset.tint).toBeUndefined();

    unmount();
  });

  it('cleans up page tint when transitioning between playlists or when color is null', () => {
    useViolenceStyle.getState().setEnabled(false);
    const pink: ArtworkColor = { background: 'rgb(255, 105, 180)', dark: true };
    const hook = renderHook(pink);

    expect(pageElement.style.getPropertyValue('--page-tint')).toBe('rgb(255, 105, 180)');

    // Switched to playlist with no color or loading
    hook.update(null);
    expect(pageElement.style.getPropertyValue('--page-tint')).toBe('');
    expect(pageElement.dataset.tint).toBeUndefined();

    hook.unmount();
  });
});

describe('useArtworkColor', () => {
  it('does not leak color when url changes or becomes null', async () => {
    const { useArtworkColor } = await import('./artworkColor');
    const container = document.createElement('div');
    document.body.appendChild(container);
    const root = createRoot(container);

    let observedColor: ArtworkColor | null = null;
    function TestComponent({ url }: { url: string | null }) {
      const color = useArtworkColor(url);
      observedColor = color;
      return null;
    }

    // Mount with null url
    act(() => {
      root.render(<TestComponent url={null} />);
    });
    expect(observedColor).toBeNull();

    // When switched to null from an unknown url, it evaluates to null
    act(() => {
      root.render(<TestComponent url="https://example.com/art1.jpg" />);
    });
    // While loading, color is null (not stale color)
    expect(observedColor).toBeNull();

    // Switch to another url
    act(() => {
      root.render(<TestComponent url="https://example.com/art2.jpg" />);
    });
    expect(observedColor).toBeNull();

    // Switch back to null
    act(() => {
      root.render(<TestComponent url={null} />);
    });
    expect(observedColor).toBeNull();

    act(() => {
      root.unmount();
    });
    document.body.removeChild(container);
  });
});

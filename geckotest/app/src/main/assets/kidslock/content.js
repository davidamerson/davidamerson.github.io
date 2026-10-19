(() => {
  'use strict';

  const css = `
    html, body { margin:0 !important; padding:0 !important; background:#000 !important; overflow:hidden !important; width:100% !important; height:100% !important; }

    /* Everything outside the selected video's player is hidden. */
    ytd-masthead, #masthead, #masthead-container, ytm-mobile-topbar-renderer,
    #secondary, #below, #related, #comments, ytd-comments, ytm-comments-entry-point-header-renderer,
    ytd-watch-next-secondary-results-renderer, ytm-watch-next-secondary-results-renderer,
    ytm-item-section-renderer, ytd-merch-shelf-renderer, #chat-container, #panels,
    ytd-watch-metadata, ytm-slim-video-metadata-section-renderer, ytm-video-with-context-renderer,
    ytd-popup-container, tp-yt-paper-dialog,

    /* Recommendations/cards/links inside and after the player. */
    .ytp-ce-element, .ytp-endscreen-content, .ytp-suggested-action,
    .ytp-endscreen-previous, .ytp-endscreen-next,
    .ytp-pause-overlay, .ytp-pause-overlay-container,
    .ytp-cards-teaser, .ytp-cards-button,
    .ytp-title, .ytp-title-text, .ytp-title-link,
    .ytp-chrome-top, .ytp-watermark, .ytp-youtube-button, .ytp-impression-link {
      display:none !important;
      visibility:hidden !important;
      pointer-events:none !important;
    }

    /* Keep the actual YouTube player and its basic controls filling our app window. */
    .html5-video-player {
      position:fixed !important;
      inset:0 !important;
      width:100vw !important;
      height:100vh !important;
      max-width:none !important;
      max-height:none !important;
      z-index:2147483646 !important;
      background:#000 !important;
    }

    .html5-video-container, video.html5-main-video {
      max-width:100vw !important;
      max-height:100vh !important;
    }
  `;

  function ensureStyle() {
    if (document.getElementById('kids-lock-style')) return;
    const style = document.createElement('style');
    style.id = 'kids-lock-style';
    style.textContent = css;
    (document.head || document.documentElement).appendChild(style);
  }

  let boundVideo = null;

  function disableAutoplay() {
    const toggle = document.querySelector('.ytp-autonav-toggle-button');
    if (toggle && toggle.getAttribute('aria-checked') === 'true') {
      try { toggle.click(); } catch (_) {}
    }
  }

  function hideRecommendations() {
    document.querySelectorAll(
      '.ytp-ce-element,.ytp-endscreen-content,.ytp-suggested-action,' +
      '.ytp-endscreen-previous,.ytp-endscreen-next,.ytp-pause-overlay,' +
      '.ytp-pause-overlay-container,.ytp-cards-teaser,.ytp-cards-button'
    ).forEach(el => {
      el.style.setProperty('display','none','important');
      el.style.setProperty('pointer-events','none','important');
    });
  }

  function bindVideo(video) {
    if (!video || video === boundVideo) return;
    boundVideo = video;
    video.autoplay = false;
    video.addEventListener('ended', () => {
      try { video.pause(); } catch (_) {}
      hideRecommendations();
    }, true);
  }

  function lockPage() {
    ensureStyle();
    const player = document.querySelector('.html5-video-player');
    if (player) {
      player.style.setProperty('position','fixed','important');
      player.style.setProperty('inset','0','important');
      player.style.setProperty('width','100vw','important');
      player.style.setProperty('height','100vh','important');
      player.style.setProperty('z-index','2147483646','important');
      player.style.setProperty('background','#000','important');
    }
    bindVideo(document.querySelector('video.html5-main-video, video'));
    disableAutoplay();
    hideRecommendations();
  }

  /* Only ordinary player controls are clickable. Everything that tries to
     navigate elsewhere is blocked before YouTube can act on it. */
  document.addEventListener('click', ev => {
    const closest = ev.target && ev.target.closest ? ev.target.closest.bind(ev.target) : null;
    if (!closest) return;

    const player = closest('.html5-video-player');
    if (!player) {
      ev.preventDefault();
      ev.stopImmediatePropagation();
      return;
    }

    const navigationTarget = closest(
      'a[href],.ytp-youtube-button,.ytp-title-link,.ytp-watermark,' +
      '.ytp-ce-element,.ytp-suggested-action,.ytp-cards-button,' +
      '.ytp-endscreen-previous,.ytp-endscreen-next'
    );
    if (navigationTarget) {
      ev.preventDefault();
      ev.stopImmediatePropagation();
    }
  }, true);

  document.addEventListener('auxclick', ev => {
    ev.preventDefault();
    ev.stopImmediatePropagation();
  }, true);

  const observer = new MutationObserver(lockPage);
  observer.observe(document.documentElement, {subtree:true, childList:true});
  window.addEventListener('load', lockPage, true);
  document.addEventListener('DOMContentLoaded', lockPage, true);
  setInterval(lockPage, 750);
})();

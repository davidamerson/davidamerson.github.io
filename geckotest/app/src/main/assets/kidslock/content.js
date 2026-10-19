(() => {
  'use strict';

  const css = `
    html, body { margin:0 !important; padding:0 !important; background:#000 !important; overflow:hidden !important; width:100% !important; height:100% !important; }
    ytd-masthead, #masthead, #masthead-container, ytm-mobile-topbar-renderer,
    #secondary, #below, #related, #comments, ytd-comments, ytm-comments-entry-point-header-renderer,
    ytd-watch-next-secondary-results-renderer, ytm-watch-next-secondary-results-renderer,
    ytm-item-section-renderer, ytd-merch-shelf-renderer, #chat-container, #panels,
    ytd-watch-metadata, ytm-slim-video-metadata-section-renderer, ytm-video-with-context-renderer,
    .ytp-ce-element, .ytp-endscreen-content, .ytp-suggested-action, .ytp-cards-teaser,
    .ytp-cards-button, .ytp-paid-content-overlay, .ytp-title, .ytp-title-text,
    .ytp-chrome-top, .ytp-watermark, .ytp-youtube-button, .ytp-impression-link,
    .ytp-endscreen-previous, .ytp-endscreen-next, tp-yt-paper-dialog,
    ytd-popup-container { display:none !important; visibility:hidden !important; pointer-events:none !important; }

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

  const style = document.createElement('style');
  style.id = 'kids-lock-style';
  style.textContent = css;
  (document.documentElement || document.head).appendChild(style);

  let boundVideo = null;

  function disableAutoplay() {
    const toggle = document.querySelector('.ytp-autonav-toggle-button');
    if (toggle && toggle.getAttribute('aria-checked') === 'true') {
      try { toggle.click(); } catch (_) {}
    }
  }

  function bindVideo(video) {
    if (!video || video === boundVideo) return;
    boundVideo = video;
    video.autoplay = false;
    video.addEventListener('ended', () => {
      try { video.pause(); } catch (_) {}
      hideEndscreen();
    }, true);
  }

  function hideEndscreen() {
    document.querySelectorAll('.ytp-ce-element,.ytp-endscreen-content,.ytp-suggested-action,.ytp-endscreen-previous,.ytp-endscreen-next')
      .forEach(el => { el.style.setProperty('display','none','important'); });
  }

  function lockPage() {
    if (!document.getElementById('kids-lock-style')) {
      (document.documentElement || document.head).appendChild(style.cloneNode(true));
    }
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
    hideEndscreen();
  }

  document.addEventListener('click', ev => {
    const player = ev.target && ev.target.closest ? ev.target.closest('.html5-video-player') : null;
    if (!player) {
      ev.preventDefault();
      ev.stopImmediatePropagation();
      return;
    }
    const link = ev.target.closest && ev.target.closest('a[href]');
    const youtubeJump = ev.target.closest && ev.target.closest('.ytp-youtube-button,.ytp-title-link,.ytp-watermark');
    if (link || youtubeJump) {
      ev.preventDefault();
      ev.stopImmediatePropagation();
    }
  }, true);

  document.addEventListener('auxclick', ev => {
    ev.preventDefault();
    ev.stopImmediatePropagation();
  }, true);

  const observer = new MutationObserver(lockPage);
  observer.observe(document.documentElement, {subtree:true, childList:true, attributes:false});

  window.addEventListener('load', lockPage, true);
  document.addEventListener('DOMContentLoaded', lockPage, true);
  setInterval(lockPage, 1000);
})();

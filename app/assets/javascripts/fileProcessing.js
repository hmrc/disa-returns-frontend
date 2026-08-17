(function () {
  'use strict';

  const container = document.getElementById('file-processing');

  if (!container) {
    return;
  }

  const pageTitle = container.getAttribute('data-page-title');
  if (pageTitle) {
    document.title = pageTitle;
  }

  const statusUrl = container.getAttribute('data-status-url');
  const pollIntervalMs = Number(container.getAttribute('data-poll-interval-millis'));
  const maxConsecutiveErrors = 5;
  const maxWaitMinutes = Number(container.getAttribute('data-max-wait-minutes'));
  const maxWaitMs = maxWaitMinutes * 60 * 1000;
  const startedAt = Date.now();
  let consecutiveErrors = 0;

  function poll() {
    window
      .fetch(statusUrl)
      .then(function (response) {
        if (!response.ok) {
          throw new Error('Unexpected status: ' + response.status);
        }

        return response.json();
      })
      .then(function (body) {
        consecutiveErrors = 0;

        if (body.redirectUrl) {
          window.location.href = body.redirectUrl;
          return;
        }

        if (body.processing === true) {
          scheduleNextPoll();
          return;
        }

        window.location.href = container.getAttribute('data-failed-url');
      })
      .catch(handleError);
  }

  function handleError() {
    consecutiveErrors += 1;

    if (consecutiveErrors >= maxConsecutiveErrors) {
      window.location.href = container.getAttribute('data-failed-url');
      return;
    }

    scheduleNextPoll();
  }

  function scheduleNextPoll() {
    if (Date.now() - startedAt >= maxWaitMs) {
      window.location.href = container.getAttribute('data-failed-url');
      return;
    }

    window.setTimeout(poll, pollIntervalMs);
  }

  poll();
})();

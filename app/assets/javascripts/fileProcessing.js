(function () {
  'use strict';

  var container = document.getElementById('file-processing');

  if (!container) {
    return;
  }

  var statusUrl = container.getAttribute('data-status-url');
  var pollIntervalMs = 2000;
  var maxConsecutiveErrors = 5;
  var consecutiveErrors = 0;

  var pendingStatuses = ['CREATED', 'UPSCAN_SUCCESS'];

  var destinationByStatus = {
    UPSCAN_QUARANTINE: container.getAttribute('data-virus-url'),
    UPSCAN_REJECTED: container.getAttribute('data-rejected-url'),
    UPSCAN_UNKNOWN: container.getAttribute('data-failed-url'),
    UPSCAN_EXPIRED: container.getAttribute('data-failed-url'),
    DUPLICATE: container.getAttribute('data-duplicate-url'),
    VALIDATION_SUCCESS: container.getAttribute('data-success-url'),
    VALIDATION_FAILURE: container.getAttribute('data-validation-errors-url')
  };

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

        var status = body.status;

        if (pendingStatuses.includes(status)) {
          scheduleNextPoll();
          return;
        }

        window.location.href = destinationByStatus[status] || container.getAttribute('data-failed-url');
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
    window.setTimeout(poll, pollIntervalMs);
  }

  poll();
})();

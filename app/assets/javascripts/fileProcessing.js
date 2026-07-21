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
    var xhr = new XMLHttpRequest();
    xhr.open('GET', statusUrl, true);

    xhr.onload = function () {
      if (xhr.status !== 200) {
        handleError();
        return;
      }

      var status;
      try {
        status = JSON.parse(xhr.responseText).status;
      } catch (e) {
        handleError();
        return;
      }

      consecutiveErrors = 0;

      if (pendingStatuses.indexOf(status) !== -1) {
        scheduleNextPoll();
        return;
      }

      window.location.href = destinationByStatus[status] || container.getAttribute('data-failed-url');
    };

    xhr.onerror = handleError;

    xhr.send();
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

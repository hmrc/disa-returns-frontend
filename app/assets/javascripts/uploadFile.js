(function () {
  'use strict';

  var form = document.getElementById('file-upload-form');

  if (!form) {
    return;
  }

  var contentSection = document.getElementById('upload-file-content');
  var progressSection = document.getElementById('upload-in-progress');
  var progressHeading = document.getElementById('upload-in-progress-heading');
  var errorRedirectUrl = form.getAttribute('data-error-redirect');
  var processingUrl = form.getAttribute('data-processing-url');
  var minFileSize = Number(form.getAttribute('data-min-file-size'));
  var maxFileSize = Number(form.getAttribute('data-max-file-size'));

  var errorSummary = document.getElementById('js-error-summary');
  var errorSummaryLink = document.getElementById('js-error-summary-link');
  var fileError = document.getElementById('js-file-error');
  var fileErrorMessage = document.getElementById('js-file-error-message');
  var formGroup = form.querySelector('.govuk-form-group');

  // The govuk-frontend file upload JS enhancement moves the original id onto its own button and
  // renames the real underlying <input type="file"> to "<id>-input", so it has to be looked up by
  // type here rather than by id.
  var fileInput = form.querySelector('input[type="file"]');

  // Without this, the browser's own native "select a file" validation intercepts the click and
  // blocks the submit event entirely when no file is chosen - it never reaches the handler below, so
  // our own inline error can never be shown. novalidate disables native constraint validation UI for
  // the whole form so our own submit-time checks are always the ones in control.
  fileInput.removeAttribute('required');
  form.setAttribute('novalidate', 'novalidate');

  var acceptedMimeTypes = (fileInput.getAttribute('accept') || '')
    .split(',')
    .map(function (type) {
      return type.trim();
    })
    .filter(function (type) {
      return type.length > 0;
    });

  var messageByCode = {
    InvalidArgument: form.getAttribute('data-message-invalidargument'),
    EntityTooSmall: form.getAttribute('data-message-invalidargument'),
    UnexpectedContent: form.getAttribute('data-message-rejected'),
    EntityTooLarge: form.getAttribute('data-message-entitytoolarge')
  };

  // Upscan's own synchronous checks (file too large/small, wrong type) can no longer be observed
  // from script for this cross-origin endpoint - see the CORS investigation. These checks reproduce
  // them client-side instead, and the result is shown inline, with no navigation at all, so the user
  // can just pick another file and resubmit on the same page.
  function validationErrorCode() {
    var files = fileInput.files;

    if (!files || files.length === 0) {
      return 'InvalidArgument';
    }

    var file = files[0];

    if (acceptedMimeTypes.length > 0 && acceptedMimeTypes.indexOf(file.type) === -1) {
      return 'UnexpectedContent';
    }

    if (file.size > maxFileSize) {
      return 'EntityTooLarge';
    }

    if (file.size < minFileSize) {
      return 'EntityTooSmall';
    }

    return null;
  }

  function showInlineError(code) {
    var message = messageByCode[code];

    errorSummaryLink.textContent = message;
    errorSummary.removeAttribute('hidden');
    errorSummary.focus();

    fileErrorMessage.textContent = message;
    fileError.removeAttribute('hidden');

    if (formGroup) {
      formGroup.classList.add('govuk-form-group--error');
    }

    fileInput.classList.add('govuk-file-upload--error');
    fileInput.setAttribute('aria-describedby', fileError.id);
  }

  function hideInlineError() {
    errorSummary.setAttribute('hidden', 'hidden');
    fileError.setAttribute('hidden', 'hidden');

    if (formGroup) {
      formGroup.classList.remove('govuk-form-group--error');
    }

    fileInput.classList.remove('govuk-file-upload--error');
    fileInput.removeAttribute('aria-describedby');
  }

  form.addEventListener('submit', function (event) {
    event.preventDefault();

    var errorCode = validationErrorCode();

    if (errorCode) {
      showInlineError(errorCode);
      return;
    }

    hideInlineError();

    contentSection.setAttribute('hidden', 'hidden');
    progressSection.removeAttribute('hidden');
    progressHeading.setAttribute('tabindex', '-1');
    progressHeading.focus();

    // mode: 'no-cors' is required here - a readable (default 'cors') fetch gets its promise rejected
    // somewhere along Upscan's redirect chain, even though the request itself completes at the
    // network level, so the response is opaque and its outcome can't be read from script. Having
    // already validated the file client-side above, any Upscan-side rejection that slips past that
    // is not distinguished here - it proceeds to the processing page like a success would, which
    // polls our backend and falls back to a generic failure if nothing ever resolves.
    window
      .fetch(form.action, {
        method: 'POST',
        body: new FormData(form),
        mode: 'no-cors'
      })
      .then(function () {
        window.location.href = processingUrl;
      })
      .catch(function () {
        window.location.href = errorRedirectUrl;
      });
  });
})();

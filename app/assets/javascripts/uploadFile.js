(function () {
  'use strict';

  var form = document.getElementById('file-upload-form');

  if (!form) {
    return;
  }

  var contentSection = document.getElementById('upload-file-content');
  var progressSection = document.getElementById('upload-in-progress');
  var progressHeading = document.getElementById('upload-in-progress-heading');
  var liveRegion = document.getElementById('upload-live-region');
  var errorRedirectUrl = form.getAttribute('data-error-redirect');
  var processingUrl = form.getAttribute('data-processing-url');
  var minFileSize = Number(form.getAttribute('data-min-file-size'));
  var maxFileSize = Number(form.getAttribute('data-max-file-size'));

  var errorSummary = document.getElementById('js-error-summary');
  var errorSummaryLink = document.getElementById('js-error-summary-link');
  var fileError = document.getElementById('js-file-error');
  var fileErrorMessage = document.getElementById('js-file-error-message');
  var formGroup = form.querySelector('.govuk-form-group');
  var fileInput = form.querySelector('input[type="file"]');

  // Without this, the browser's own native "select a file" validation intercepts the click and
  // blocks the submit event entirely when no file is chosen - it never reaches the handler below, so
  // our own inline error can never be shown. novalidate disables validation for
  // the whole form so our own submit checks are always the ones in control.
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

  function validationErrorCode() {
    var files = fileInput.files;

    if (!files || files.length === 0) {
      return 'InvalidArgument';
    }

    var file = files[0];

    if (acceptedMimeTypes.length > 0 && !acceptedMimeTypes.includes(file.type)) {
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

    window.setTimeout(function () {
      liveRegion.textContent = progressSection.textContent;
    }, 100);

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

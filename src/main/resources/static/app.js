(function () {
  'use strict';

  function $(id) { return document.getElementById(id); }

  /* ---------- Dark / light theme ---------- */
  var root = document.documentElement;
  var themeBtn = $('theme');
  if (themeBtn) {
    themeBtn.addEventListener('click', function () {
      var next = root.getAttribute('data-theme') === 'dark' ? 'light' : 'dark';
      root.setAttribute('data-theme', next);
      try { localStorage.setItem('theme', next); } catch (e) { /* ignore */ }
    });
  }

  /* ---------- Upload box: choose file or drag and drop ---------- */
  var MAX_BYTES = 5 * 1024 * 1024;
  var input = $('file');
  var drop = $('drop');
  var fileLabel = $('file-name');
  var clientError = $('client-error');
  var defaultText = 'PDF only \u00B7 max 5 MB \u00B7 processed in memory';

  function showError(message) {
    clientError.textContent = message;
    clientError.hidden = !message;
  }

  function refreshFile() {
    showError('');
    var f = input.files && input.files[0];
    if (!f) {
      fileLabel.textContent = defaultText;
      drop.classList.remove('has-file');
      return;
    }
    if (f.type !== 'application/pdf' && !/\.pdf$/i.test(f.name)) {
      input.value = '';
      fileLabel.textContent = defaultText;
      drop.classList.remove('has-file');
      showError('Please choose a PDF file.');
      return;
    }
    if (f.size > MAX_BYTES) {
      input.value = '';
      fileLabel.textContent = defaultText;
      drop.classList.remove('has-file');
      showError('This file is larger than 5 MB. Please choose a smaller PDF.');
      return;
    }
    fileLabel.textContent = f.name + ' \u00B7 ' + Math.max(1, Math.round(f.size / 1024)) + ' KB';
    drop.classList.add('has-file');
  }

  if (input && drop) {
    input.addEventListener('change', refreshFile);
    ['dragenter', 'dragover'].forEach(function (name) {
      drop.addEventListener(name, function (e) { e.preventDefault(); drop.classList.add('drag'); });
    });
    ['dragleave', 'drop'].forEach(function (name) {
      drop.addEventListener(name, function (e) { e.preventDefault(); drop.classList.remove('drag'); });
    });
    drop.addEventListener('drop', function (e) {
      if (e.dataTransfer && e.dataTransfer.files.length) {
        input.files = e.dataTransfer.files;
        refreshFile();
      }
    });
  }

  /* ---------- Loading screen while the AI works ---------- */
  var form = $('form');
  var loading = $('loading');
  var tip = $('loading-tip');
  var goBtn = $('go');
  var tips = [
    'Reading the text from your PDF in memory...',
    'Asking the local AI model to find your skills...',
    'Comparing your skills with the target role...',
    'Searching the knowledge base for learning resources...',
    'Writing your personalized roadmap...'
  ];
  var tipTimer = null;

  if (form) {
    form.addEventListener('submit', function () {
      loading.hidden = false;
      goBtn.disabled = true;
      var i = 0;
      tip.textContent = tips[0];
      tipTimer = setInterval(function () {
        i = (i + 1) % tips.length;
        tip.textContent = tips[i];
      }, 6000);
    });
  }

  // If the user presses the Back button, make sure the loading screen is gone
  window.addEventListener('pageshow', function () {
    if (loading) { loading.hidden = true; }
    if (goBtn) { goBtn.disabled = false; }
    if (tipTimer) { clearInterval(tipTimer); }
  });

  /* ---------- Result tabs ---------- */
  var tabs = document.querySelectorAll('.tab');
  tabs.forEach(function (tab) {
    tab.addEventListener('click', function () {
      tabs.forEach(function (t) {
        t.classList.remove('active');
        t.setAttribute('aria-selected', 'false');
      });
      document.querySelectorAll('.panel').forEach(function (p) { p.hidden = true; });
      tab.classList.add('active');
      tab.setAttribute('aria-selected', 'true');
      $(tab.getAttribute('data-target')).hidden = false;
    });
  });

  /* ---------- Roadmap progress (kept only in this page, never saved) ---------- */
  var checks = document.querySelectorAll('.step-check');
  function updateProgress() {
    var done = document.querySelectorAll('.step-check:checked').length;
    var total = checks.length;
    var bar = $('pbar');
    var text = $('ptext');
    if (bar) { bar.style.width = (total ? (done * 100 / total) : 0) + '%'; }
    if (text) { text.textContent = done + ' of ' + total + ' steps done'; }
  }
  checks.forEach(function (c) { c.addEventListener('change', updateProgress); });
  updateProgress();

  /* ---------- Filter the skills list ---------- */
  var search = $('skill-search');
  if (search) {
    search.addEventListener('input', function () {
      var q = search.value.trim().toLowerCase();
      document.querySelectorAll('#skill-chips .chip').forEach(function (chip) {
        chip.hidden = !!q && chip.textContent.toLowerCase().indexOf(q) === -1;
      });
    });
  }

  /* ---------- Copy buttons ---------- */
  function setupCopy(buttonId, getText) {
    var btn = $(buttonId);
    if (!btn) { return; }
    btn.addEventListener('click', function () {
      var original = btn.textContent;
      var copied = navigator.clipboard
        ? navigator.clipboard.writeText(getText())
        : Promise.reject(new Error('Clipboard not available'));
      copied.then(
        function () { btn.textContent = 'Copied \u2713'; },
        function () { btn.textContent = 'Copy failed'; }
      ).then(function () {
        setTimeout(function () { btn.textContent = original; }, 1500);
      });
    });
  }
  setupCopy('copy-skills', function () {
    return Array.prototype.map.call(
      document.querySelectorAll('#skill-chips .chip'),
      function (c) { return c.textContent; }
    ).join(', ');
  });
  setupCopy('copy-raw', function () { return $('raw-text').textContent; });

  /* ---------- AI coach: request only the coach fragment, without reloading results ---------- */
  var coachForm = $('coach-form');
  if (coachForm) {
    coachForm.addEventListener('submit', function (event) {
      event.preventDefault();
      var button = $('coach-submit');
      var result = $('coach-result');
      var original = button.textContent;
      button.disabled = true;
      button.textContent = 'Working...';
      result.textContent = 'The local AI coach is preparing suggestions...';
      fetch(coachForm.action, { method: 'POST', body: new FormData(coachForm) })
        .then(function (response) {
          if (!response.ok) { throw new Error('The coach request failed. Please try again.'); }
          return response.text();
        })
        .then(function (html) { result.innerHTML = html; })
        .catch(function (error) { result.textContent = error.message; })
        .finally(function () {
          button.disabled = false;
          button.textContent = original;
        });
    });
  }

  /* ---------- After analysis, scroll down to the results ---------- */
  var results = $('results');
  if (results) {
    results.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }
})();
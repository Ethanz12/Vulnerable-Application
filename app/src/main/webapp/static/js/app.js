/* Shared client helpers for the Campus Events app. */
(function () {
  'use strict';

  window.App = {
    async api(path, body, method) {
      var opts = {
        method: method || (body ? 'POST' : 'GET'),
        credentials: 'same-origin',
        headers: { 'Content-Type': 'application/json' }
      };
      if (body) {
        opts.body = AppCrypto.encryptBody(body);
      }
      var raw = await fetch(path, opts);
      var envelope;
      try {
        envelope = await raw.json();
      } catch (e) {
        throw new Error('HTTP ' + raw.status);
      }
      var data = AppCrypto.decryptBody(envelope);
      if (!raw.ok || data.ok === false) {
        var err = new Error(data.error || ('HTTP ' + raw.status));
        err.status = raw.status;
        throw err;
      }
      return data;
    },

    toast(message, isError) {
      var t = document.getElementById('toast');
      if (!t) { return; }
      t.textContent = message;
      t.classList.toggle('error', !!isError);
      t.classList.add('show');
      clearTimeout(t._timer);
      t._timer = setTimeout(function () { t.classList.remove('show'); }, 5000);
    },

    esc(s) {
      return String(s == null ? '' : s).replace(/[&<>"']/g, function (c) {
        return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c];
      });
    },

    fileToB64(file) {
      return new Promise(function (resolve, reject) {
        var r = new FileReader();
        r.onload = function () {
          resolve(r.result.split(',', 2)[1]);
        };
        r.onerror = reject;
        r.readAsDataURL(file);
      });
    }
  };

  var logout = document.getElementById('logout-link');
  if (logout) {
    logout.addEventListener('click', async function (e) {
      e.preventDefault();
      try { await App.api('api/auth', { action: 'logout' }); } catch (err) { /* ignore */ }
      location.href = 'index.jsp';
    });
  }
})();

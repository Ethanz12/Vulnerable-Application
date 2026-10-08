<%@ include file="WEB-INF/jsp/header.jspf" %>
<div class="panel" style="max-width:36rem;margin:0 auto">
  <h1>Account activation</h1>
  <p class="muted">New registrations stay pending until activated. The activation code is derived from the account's <span class="code">student ID</span> and its <span class="code">registration epoch</span>; both are listed on the public orientation directory below.</p>
  <form id="activate-form">
    <label for="act-token">Activation code</label>
    <input type="text" id="act-token" placeholder="Enter your activation code" required>
    <label for="act-password">New password <span class="muted">(min 8 characters)</span></label>
    <input type="password" id="act-password" autocomplete="new-password" placeholder="Choose a strong password" required minlength="8">
    <button type="submit" id="activate-btn" style="width:100%">Activate account</button>
  </form>
  <p id="act-account" class="muted form-note"></p>
</div>

<div class="panel" style="max-width:36rem;margin:1rem auto 0">
  <h2>Pending registrations <span class="muted">(orientation directory)</span></h2>
  <table id="pending-table">
    <thead><tr><th>Username</th><th>Student ID</th><th>Registered (approx.)</th></tr></thead>
    <tbody><tr><td colspan="3" class="muted">Loading...</td></tr></tbody>
  </table>
  <details class="advanced">
    <summary>How activation codes work</summary>
    <p class="muted">The registrar derives codes as <span class="code">md5("ACTIVATE:&lt;student_id&gt;:&lt;epoch&gt;")[0:8]</span>. The directory lists the approximate registration time for each pending account.</p>
  </details>
</div>
<script src="static/js/app.js"></script>
<script>
(function () {
  var tokenInput = document.getElementById('act-token');
  var token = new URLSearchParams(location.search).get('token');
  if (token) { tokenInput.value = token; }
  tokenInput.addEventListener('change', resolveToken);
  if (token) { resolveToken(); }

  async function resolveToken() {
    if (!tokenInput.value) { return; }
    try {
      var data = await App.api('api/activate?token=' + encodeURIComponent(tokenInput.value));
      document.getElementById('act-account').textContent =
        'Account found: ' + data.username + ' (' + data.student_id + ') — choose a new password.';
    } catch (e) {
      document.getElementById('act-account').textContent = e.message;
    }
  }

  document.getElementById('activate-form').addEventListener('submit', async function (e) {
    e.preventDefault();
    var btn = document.getElementById('activate-btn');
    btn.disabled = true;
    btn.textContent = 'Activating...';
    try {
      var data = await App.api('api/activate', {
        token: tokenInput.value,
        password: document.getElementById('act-password').value
      });
      App.toast('Account activated! Redirecting...');
      setTimeout(function () { location.href = data.redirect; }, 900);
    } catch (err) {
      App.toast(err.message, true);
      btn.disabled = false;
      btn.textContent = 'Activate account';
    }
  });

  App.api('api/directory/pending').then(function (data) {
    var rows = data.pending.map(function (p) {
      var epoch = p.registered_hour_epoch || p.registered_epoch || '';
      return '<tr><td>' + App.esc(p.username) + '</td><td><span class="code">' + App.esc(p.student_id) + '</span></td><td><span class="code">' + epoch + '</span></td></tr>';
    });
    document.querySelector('#pending-table tbody').innerHTML =
      rows.length ? rows.join('') : '<tr><td colspan="3" class="muted">No pending registrations.</td></tr>';
  }).catch(function (e) {
    document.querySelector('#pending-table tbody').innerHTML =
      '<tr><td colspan="3" class="muted">' + App.esc(e.message) + '</td></tr>';
  });
})();
</script>
<%@ include file="WEB-INF/jsp/footer.jspf" %>

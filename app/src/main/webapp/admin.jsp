<%
    campus.web.User pageUser = (campus.web.User) session.getAttribute("user");
    if (pageUser == null) { response.sendRedirect("login.jsp"); return; }
    if (!pageUser.isAdmin()) { response.sendRedirect("index.jsp"); return; }
%>
<%@ include file="WEB-INF/jsp/header.jspf" %>
<h1>Administration</h1>
<div class="tabs">
  <button data-tab="review" class="on">Review queue</button>
  <button data-tab="users">Users &amp; roles</button>
  <button data-tab="announcements">Announcements</button>
  <button data-tab="signage">Signage</button>
  <button data-tab="audit">Audit log</button>
</div>

<section id="tab-review" class="panel">
  <h2>Events pending review</h2>
  <table id="review-table">
    <thead><tr><th>ID</th><th>Title</th><th>Organiser</th><th></th></tr></thead>
    <tbody><tr><td colspan="4" class="muted">Loading...</td></tr></tbody>
  </table>
</section>

<section id="tab-users" class="panel" hidden>
  <h2>Users &amp; roles</h2>
  <table id="users-table">
    <thead><tr><th>ID</th><th>Username</th><th>Role</th><th>Status</th><th></th></tr></thead>
    <tbody><tr><td colspan="5" class="muted">Loading...</td></tr></tbody>
  </table>
</section>

<section id="tab-announcements" class="panel" hidden>
  <h2>Announcement templates</h2>
  <table id="ann-table">
    <thead><tr><th>ID</th><th>Name</th><th>Body</th><th></th></tr></thead>
    <tbody><tr><td colspan="4" class="muted">Loading...</td></tr></tbody>
  </table>
  <div style="margin-top:1.25rem;max-width:36rem;border-top:1px solid var(--border);padding-top:1rem">
    <h3 style="margin-bottom:0.5rem">Add template</h3>
    <form id="ann-form">
      <label for="ann-name">Template name</label>
      <input type="text" id="ann-name" placeholder="e.g. Welcome banner" required>
      <label for="ann-body">Body (HTML)</label>
      <textarea id="ann-body" style="min-height:6rem" placeholder="<p>Template content...</p>"></textarea>
      <button type="submit">Add template</button>
    </form>
  </div>
</section>

<section id="tab-signage" class="panel" hidden>
  <h2>Signage templates</h2>
  <p class="muted">Packaged templates live under <span class="code">WEB-INF/templates/signage</span>.
     Uploaded templates are stored under <span class="code">uploads/templates</span> and can be
     previewed by server-side path.</p>
  <div style="max-width:36rem;border:1px dashed var(--border);border-radius:var(--radius-md);padding:1.25rem;margin:1rem 0;text-align:center">
    <form id="upload-form">
      <label for="up-file" style="margin-top:0">Upload template file</label>
      <input type="file" id="up-file" required>
      <button type="submit" id="upload-btn">Upload</button>
    </form>
  </div>
  <h3>Packaged</h3>
  <ul id="packaged-list" class="muted" style="padding-left:1.25rem"></ul>
  <h3>Uploaded</h3>
  <ul id="uploads-list" class="muted" style="padding-left:1.25rem"></ul>
</section>

<section id="tab-audit" class="panel" hidden>
  <h2>Audit log</h2>
  <table id="audit-table">
    <thead><tr><th>When (UTC)</th><th>Actor</th><th>Action</th><th>Detail</th></tr></thead>
    <tbody><tr><td colspan="4" class="muted">Loading...</td></tr></tbody>
  </table>
</section>
<script src="static/js/app.js"></script>
<script>
var tabs = document.querySelectorAll('.tabs button');
tabs.forEach(function (b) {
  b.addEventListener('click', function () {
    tabs.forEach(function (x) { x.classList.toggle('on', x === b); });
    document.querySelectorAll('section[id^="tab-"]').forEach(function (s) {
      s.hidden = s.id !== 'tab-' + b.dataset.tab;
    });
  });
});

async function loadReview() {
  var tbody = document.querySelector('#review-table tbody');
  try {
    var data = await App.api('api/admin/review');
    tbody.innerHTML = data.pending.length ? data.pending.map(function (ev) {
      return '<tr><td class="muted">#' + ev.id + '</td><td style="font-weight:500">' + App.esc(ev.title) + '</td><td>' + App.esc(ev.organiser)
        + '</td><td style="text-align:right"><a class="btn" href="review.jsp?id=' + ev.id + '">Review</a></td></tr>';
    }).join('') : '<tr><td colspan="4" class="muted" style="text-align:center;padding:1.5rem">Queue is empty.</td></tr>';
  } catch (e) {
    tbody.innerHTML = '<tr><td colspan="4" class="muted">' + App.esc(e.message) + '</td></tr>';
  }
}

var USERS_ROLES = ['student', 'organiser', 'admin'];
async function loadUsers() {
  var tbody = document.querySelector('#users-table tbody');
  try {
    var data = await App.api('api/admin/users');
    tbody.innerHTML = data.users.map(function (usr) {
      var sel = '<select data-id="' + usr.id + '">'
        + USERS_ROLES.map(function (r) {
            return '<option value="' + r + '"' + (r === usr.role ? ' selected' : '') + '>' + r + '</option>';
          }).join('')
        + '</select>';
      return '<tr><td class="muted">#' + usr.id + '</td><td style="font-weight:500">' + App.esc(usr.username) + '</td><td>' + sel
        + '</td><td><span class="chip ' + App.esc(usr.status) + '">' + App.esc(usr.status) + '</span></td>'
        + '<td style="text-align:right"><button class="secondary" data-id="' + usr.id + '">Apply</button></td></tr>';
    }).join('');
    tbody.querySelectorAll('button').forEach(function (b) {
      b.addEventListener('click', async function () {
        var id = parseInt(b.dataset.id, 10);
        var role = tbody.querySelector('select[data-id="' + id + '"]').value;
        try {
          var out = await App.api('api/admin/users', { user_id: id, role: role });
          App.toast(out.username + ' is now ' + out.role);
          loadUsers();
        } catch (e) { App.toast(e.message, true); }
      });
    });
  } catch (e) {
    tbody.innerHTML = '<tr><td colspan="5" class="muted">' + App.esc(e.message) + '</td></tr>';
  }
}

async function loadAnnouncements() {
  var tbody = document.querySelector('#ann-table tbody');
  try {
    var data = await App.api('api/admin/announcements');
    tbody.innerHTML = data.templates.length ? data.templates.map(function (t) {
      return '<tr><td class="muted">#' + t.id + '</td><td style="font-weight:500">' + App.esc(t.name) + '</td>'
        + '<td class="muted">' + App.esc(t.body_html.substring(0, 80)) + (t.body_html.length > 80 ? '...' : '') + '</td>'
        + '<td style="text-align:right"><button class="danger" data-id="' + t.id + '">Delete</button></td></tr>';
    }).join('') : '<tr><td colspan="4" class="muted" style="text-align:center;padding:1.5rem">No templates.</td></tr>';
    tbody.querySelectorAll('button').forEach(function (b) {
      b.addEventListener('click', async function () {
        try {
          await App.api('api/admin/announcements', { action: 'delete', id: parseInt(b.dataset.id, 10) });
          loadAnnouncements();
        } catch (e) { App.toast(e.message, true); }
      });
    });
  } catch (e) {
    tbody.innerHTML = '<tr><td colspan="4" class="muted">' + App.esc(e.message) + '</td></tr>';
  }
}

document.getElementById('ann-form').addEventListener('submit', async function (e) {
  e.preventDefault();
  try {
    await App.api('api/admin/announcements', {
      name: document.getElementById('ann-name').value,
      body_html: document.getElementById('ann-body').value
    });
    document.getElementById('ann-form').reset();
    App.toast('Template added');
    loadAnnouncements();
  } catch (err) { App.toast(err.message, true); }
});

async function loadSignage() {
  try {
    var data = await App.api('api/admin/templates');
    document.getElementById('packaged-list').innerHTML = data.packaged.map(function (p) {
      return '<li><span class="code">' + App.esc(p) + '</span> '
        + '<a href="admin/templates/preview?path=' + encodeURIComponent(p) + '" target="_blank">preview</a></li>';
    }).join('') || '<li>none</li>';
    document.getElementById('uploads-list').innerHTML = data.uploads.map(function (p) {
      return '<li><span class="code">' + App.esc(p) + '</span> '
        + '<a href="admin/templates/preview?path=' + encodeURIComponent(p) + '" target="_blank">preview</a></li>';
    }).join('') || '<li>nothing uploaded yet</li>';
  } catch (e) { App.toast(e.message, true); }
}

document.getElementById('upload-form').addEventListener('submit', async function (e) {
  e.preventDefault();
  var f = document.getElementById('up-file').files[0];
  if (!f) { return; }
  var btn = document.getElementById('upload-btn');
  btn.disabled = true;
  btn.textContent = 'Uploading...';
  try {
    var b64 = await App.fileToB64(f);
    var data = await App.api('api/admin/templates', { filename: f.name, content_b64: b64 });
    App.toast('Uploaded ' + data.path + ' (' + data.size + ' bytes)');
    loadSignage();
    document.getElementById('upload-form').reset();
  } catch (err) { App.toast(err.message, true); }
  btn.disabled = false;
  btn.textContent = 'Upload';
});

async function loadAudit() {
  var tbody = document.querySelector('#audit-table tbody');
  try {
    var data = await App.api('api/admin/audit');
    tbody.innerHTML = data.entries.map(function (a) {
      return '<tr><td class="muted">' + App.esc(a.at.replace('T', ' ').substring(0, 19)) + '</td>'
        + '<td style="font-weight:500">' + App.esc(a.actor || '') + '</td><td><span class="code">' + App.esc(a.action)
        + '</span></td><td>' + App.esc(a.detail || '') + '</td></tr>';
    }).join('') || '<tr><td colspan="4" class="muted" style="text-align:center;padding:1.5rem">No entries.</td></tr>';
  } catch (e) {
    tbody.innerHTML = '<tr><td colspan="4" class="muted">' + App.esc(e.message) + '</td></tr>';
  }
}

loadReview();
loadUsers();
loadAnnouncements();
loadSignage();
loadAudit();
</script>
<%@ include file="WEB-INF/jsp/footer.jspf" %>

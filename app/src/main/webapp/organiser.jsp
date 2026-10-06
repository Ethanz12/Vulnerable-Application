<%
    campus.web.User pageUser = (campus.web.User) session.getAttribute("user");
    if (pageUser == null) { response.sendRedirect("login.jsp"); return; }
    if (!pageUser.isOrganiser()) { response.sendRedirect("index.jsp"); return; }
%>
<%@ include file="WEB-INF/jsp/header.jspf" %>
<h1>Organiser workspace</h1>
<div class="panel">
  <h2>My events</h2>
  <table id="events-table">
    <thead><tr><th>ID</th><th>Title</th><th>Status</th><th></th></tr></thead>
    <tbody><tr><td colspan="4" class="muted">Loading...</td></tr></tbody>
  </table>
  <button id="new-event-btn">New event</button>
</div>

<div class="panel" id="editor-panel" hidden>
  <h2 id="editor-title">Edit event</h2>
  <form id="editor-form">
    <input type="hidden" id="ev-id">
    <label for="ev-title">Title</label>
    <input type="text" id="ev-title" required>
    <label for="ev-location">Location</label>
    <input type="text" id="ev-location">
    <label for="ev-starts">Starts (UTC)</label>
    <input type="datetime-local" id="ev-starts">
    <label for="ev-description">Description (rich text)</label>
    <textarea id="ev-description" name="description"></textarea>
    <button type="submit" id="save-btn">Save</button>
    <button type="button" class="secondary" id="submit-review-btn">Submit for administrator review</button>
    <p class="muted">Submitting notifies the administrator review service, which opens the
      draft in an authenticated review session.</p>
  </form>
</div>
<script src="static/vendor/ckeditor/ckeditor.js"></script>
<script src="static/js/app.js"></script>
<script>
var editor;
var eventsTable = document.querySelector('#events-table tbody');

function statusChip(s) { return '<span class="chip ' + App.esc(s) + '">' + App.esc(s.replace('_', ' ')) + '</span>'; }

async function loadEvents() {
  try {
    var data = await App.api('api/organiser');
    if (!data.events.length) {
      eventsTable.innerHTML = '<tr><td colspan="4" class="muted">No events yet - create one.</td></tr>';
      return;
    }
    eventsTable.innerHTML = data.events.map(function (ev) {
      return '<tr><td>#' + ev.id + '</td><td>' + App.esc(ev.title) + '</td><td>' + statusChip(ev.status)
        + '</td><td><button class="secondary" data-id="' + ev.id + '">Edit</button></td></tr>';
    }).join('');
    eventsTable.querySelectorAll('button').forEach(function (b) {
      b.addEventListener('click', function () {
        var ev = data.events.find(function (x) { return x.id === parseInt(b.dataset.id, 10); });
        openEditor(ev);
      });
    });
  } catch (e) {
    eventsTable.innerHTML = '<tr><td colspan="4" class="muted">' + App.esc(e.message) + '</td></tr>';
  }
}

function openEditor(ev) {
  document.getElementById('editor-panel').hidden = false;
  document.getElementById('editor-title').textContent = ev ? 'Edit event #' + ev.id : 'New event';
  document.getElementById('ev-id').value = ev ? ev.id : '';
  document.getElementById('ev-title').value = ev ? ev.title : '';
  document.getElementById('ev-location').value = ev && ev.location ? ev.location : '';
  document.getElementById('ev-starts').value = ev && ev.starts_at ? ev.starts_at : '';
  fillDescription(ev ? ev.id : null);
}

async function fillDescription(eventId) {
  if (!window.CKEDITOR) { return; }
  if (!editor) {
    editor = CKEDITOR.replace('ev-description', { allowedContent: true });
  }
  var html = '';
  if (eventId) {
    try {
      var data = await App.api('api/organiser?id=' + eventId);
      html = data.event.description_html || '';
    } catch (e) { html = ''; }
  }
  editor.setData(html);
}

document.getElementById('new-event-btn').addEventListener('click', function () { openEditor(null); });

document.getElementById('editor-form').addEventListener('submit', async function (e) {
  e.preventDefault();
  var payload = {
    action: 'save',
    event_id: document.getElementById('ev-id').value || '',
    title: document.getElementById('ev-title').value,
    location: document.getElementById('ev-location').value,
    starts_at: document.getElementById('ev-starts').value,
    description_html: editor ? editor.getData() : document.getElementById('ev-description').value
  };
  try {
    var data = await App.api('api/organiser', payload);
    document.getElementById('ev-id').value = data.event_id;
    App.toast('Event saved');
    loadEvents();
  } catch (err) { App.toast(err.message, true); }
});

document.getElementById('submit-review-btn').addEventListener('click', async function () {
  var id = document.getElementById('ev-id').value;
  if (!id) { App.toast('Save the event first', true); return; }
  try {
    await App.api('api/organiser', { action: 'submit', event_id: parseInt(id, 10) });
    App.toast('Submitted for review - the administrator review service has been notified');
    loadEvents();
  } catch (err) { App.toast(err.message, true); }
});

loadEvents();
</script>
<%@ include file="WEB-INF/jsp/footer.jspf" %>

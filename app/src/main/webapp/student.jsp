<%
    campus.web.User pageUser = (campus.web.User) session.getAttribute("user");
    if (pageUser == null) { response.sendRedirect("login.jsp"); return; }
%>
<%@ include file="WEB-INF/jsp/header.jspf" %>
<h1>My campus</h1>

<div class="panel" id="profile-panel"><p class="muted">Loading profile...</p></div>

<div class="panel" style="max-width:36rem">
  <h2>Register attendance</h2>
  <p class="muted">Select a published event to confirm your attendance.</p>
  <div id="attend-list"></div>
</div>

<div class="panel" style="max-width:36rem">
  <h2>Join an event team</h2>
  <p class="muted">Enter an invite code to join an event team.</p>
  <form id="join-form">
    <label for="join-token">Invite code</label>
    <input type="text" id="join-token" placeholder="INV-..." required>
    <details class="advanced">
      <summary>Advanced options</summary>
      <label for="join-event">Event ID</label>
      <input type="text" id="join-event" inputmode="numeric" placeholder="Leave empty to use code's event">
      <label for="join-role">Role</label>
      <select id="join-role">
        <option value="volunteer">Volunteer</option>
        <option value="organiser">Organiser</option>
      </select>
    </details>
    <button type="submit" id="join-btn">Join team</button>
  </form>
  <p id="join-msg" class="muted form-note"></p>
</div>
<script src="static/js/app.js"></script>
<script>
(async function () {
  var me;
  var profile = document.getElementById('profile-panel');
  var attendList = document.getElementById('attend-list');

  async function loadMe() {
    me = await App.api('api/me');
    var teamInfo = me.teams.length
      ? me.teams.map(function (t) {
          return '<span class="chip ' + App.esc(t.role) + '">' + App.esc(t.role) + '</span> '
            + App.esc(t.title) + ' (event #' + t.event_id + ')';
        }).join('<br>')
      : '<span class="muted">Not on any team yet</span>';
    var attendInfo = me.attending.length
      ? me.attending.map(function (a) { return App.esc(a.title); }).join(', ')
      : '<span class="muted">Not attending any events</span>';

    profile.innerHTML =
      '<div style="display:flex;align-items:center;gap:0.75rem;margin-bottom:0.75rem">'
      + '<div style="width:40px;height:40px;border-radius:50%;background:var(--primary-100);display:flex;align-items:center;justify-content:center;font-weight:700;color:var(--primary-700);font-size:1.1rem">'
      + App.esc(me.username.charAt(0).toUpperCase()) + '</div>'
      + '<div><h2 style="margin:0">' + App.esc(me.username) + '</h2>'
      + '<p class="muted" style="margin:0">Account #' + me.id + ' &middot; <span class="chip '
      + App.esc(me.role) + '">' + App.esc(me.role) + '</span>'
      + (me.student_id ? ' &middot; ' + App.esc(me.student_id) : '') + '</p></div></div>'
      + '<div style="display:grid;grid-template-columns:1fr 1fr;gap:1rem;margin-top:0.5rem">'
      + '<div><p class="muted" style="margin:0 0 0.25rem;font-size:0.75rem;text-transform:uppercase;letter-spacing:0.05em">Teams</p>' + teamInfo + '</div>'
      + '<div><p class="muted" style="margin:0 0 0.25rem;font-size:0.75rem;text-transform:uppercase;letter-spacing:0.05em">Attending</p>' + attendInfo + '</div>'
      + '</div>';

    if (me.role !== 'student') {
      attendList.innerHTML = '<p class="muted">Attendance registration is for student accounts only.</p>';
    }
  }

  var events;
  try {
    events = (await App.api('api/events')).events;
  } catch (e) { events = []; }

  function renderAttend() {
    var attending = new Set((me ? me.attending : []).map(function (a) { return a.id; }));
    attendList.innerHTML = events.map(function (ev) {
      return '<div style="display:flex;align-items:center;gap:0.75rem;padding:0.6rem 0;border-bottom:1px solid var(--border)">'
        + '<span style="flex:1;font-weight:500">' + App.esc(ev.title) + '</span>'
        + (attending.has(ev.id)
            ? '<span class="chip active">Attending</span>'
            : '<button class="secondary" data-id="' + ev.id + '">Attend</button>')
        + '</div>';
    }).join('') || '<p class="muted">No published events.</p>';
    attendList.querySelectorAll('button').forEach(function (b) {
      b.addEventListener('click', async function () {
        try {
          await App.api('api/attendance', { event_id: parseInt(b.dataset.id, 10) });
          App.toast('Attendance recorded');
          await loadMe();
          renderAttend();
        } catch (e) { App.toast(e.message, true); }
      });
    });
  }

  var joinToken = document.getElementById('join-token');
  var joinEvent = document.getElementById('join-event');
  var joinRole = document.getElementById('join-role');
  var joinMsg = document.getElementById('join-msg');

  joinToken.addEventListener('change', async function () {
    if (!joinToken.value) { return; }
    try {
      var info = await App.api('api/team?token=' + encodeURIComponent(joinToken.value));
      joinEvent.value = String(info.event_id);
      joinRole.value = info.allowed_role;
      joinMsg.innerHTML = 'Code valid for <strong>' + App.esc(info.event_title) + '</strong> (role: ' + App.esc(info.allowed_role) + ')';
    } catch (e) {
      joinMsg.textContent = e.message;
    }
  });

  document.getElementById('join-form').addEventListener('submit', async function (e) {
    e.preventDefault();
    var payload = { token: joinToken.value, role: joinRole.value };
    if (joinEvent.value) { payload.event_id = parseInt(joinEvent.value, 10); }
    try {
      var data = await App.api('api/team', payload);
      joinMsg.innerHTML = 'Joined event #' + data.event_id + ' as <strong>' + App.esc(data.role) + '</strong>';
      App.toast('Joined event #' + data.event_id + ' as ' + data.role);
      if (data.your_role === 'organiser') {
        setTimeout(function () { location.href = 'organiser.jsp'; }, 1200);
      } else {
        await loadMe();
        renderAttend();
      }
    } catch (err) {
      App.toast(err.message, true);
      joinMsg.textContent = err.message;
    }
  });

  try {
    await loadMe();
    renderAttend();
  } catch (e) {
    profile.innerHTML = '<div class="empty-state"><p>' + App.esc(e.message) + '</p></div>';
  }
})();
</script>
<%@ include file="WEB-INF/jsp/footer.jspf" %>

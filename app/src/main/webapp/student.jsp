<%
    campus.web.User pageUser = (campus.web.User) session.getAttribute("user");
    if (pageUser == null) { response.sendRedirect("login.jsp"); return; }
%>
<%@ include file="WEB-INF/jsp/header.jspf" %>
<h1>My campus</h1>
<div class="panel" id="profile-panel"><p class="muted">Loading...</p></div>

<div class="panel" style="max-width:34rem">
  <h2>Register attendance</h2>
  <p class="muted">Pick a published event to confirm you are attending.</p>
  <div id="attend-list"></div>
</div>

<div class="panel" style="max-width:34rem">
  <h2>Join an event team</h2>
  <form id="join-form">
    <label for="join-token">Invite code</label>
    <input type="text" id="join-token" placeholder="INV-..." required>
    <details class="advanced">
      <summary>Advanced</summary>
      <label for="join-event">Event ID</label>
      <input type="text" id="join-event" inputmode="numeric">
      <label for="join-role">Role</label>
      <select id="join-role">
        <option value="volunteer">volunteer</option>
        <option value="organiser">organiser</option>
      </select>
      <p class="muted">Leave Event ID empty to use the code's own event.</p>
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
    profile.innerHTML =
      '<h2 style="margin-top:0">' + App.esc(me.username) + '</h2>'
      + '<p class="muted">Account #' + me.id + ' &middot; role <span class="chip '
      + App.esc(me.role) + '">' + App.esc(me.role) + '</span>'
      + (me.student_id ? ' &middot; ' + App.esc(me.student_id) : '') + '</p>'
      + (me.teams.length
          ? '<p>Teams: ' + me.teams.map(function (t) {
              return App.esc(t.title) + ' (' + App.esc(t.role) + ', event #' + t.event_id + ')';
            }).join(' &middot; ') + '</p>'
          : '')
      + (me.attending.length
          ? '<p>Attending: ' + me.attending.map(function (a) { return App.esc(a.title); }).join(' &middot; ') + '</p>'
          : '');
    if (me.role !== 'student') {
      attendList.innerHTML = '<p class="muted">Attendance registration is for student accounts.</p>';
    }
  }

  var events;
  try {
    events = (await App.api('api/events')).events;
  } catch (e) { events = []; }
  function renderAttend() {
    var attending = new Set((me ? me.attending : []).map(function (a) { return a.id; }));
    attendList.innerHTML = events.map(function (ev) {
      return '<div style="display:flex;align-items:center;gap:0.6rem;margin:0.35rem 0">'
        + '<span style="flex:1">' + App.esc(ev.title) + '</span>'
        + (attending.has(ev.id)
            ? '<span class="chip active">attending</span>'
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
      joinMsg.textContent = 'Code is for "' + info.event_title + '" (role: ' + info.allowed_role + ').';
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
      joinMsg.textContent = 'Joined event #' + data.event_id + ' as ' + data.role + '.';
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
    profile.innerHTML = '<p class="muted">' + App.esc(e.message) + '</p>';
  }
})();
</script>
<%@ include file="WEB-INF/jsp/footer.jspf" %>

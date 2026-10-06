<%@ include file="WEB-INF/jsp/header.jspf" %>
<h1>Event</h1>
<div id="event-panel" class="panel"><p class="muted">Loading...</p></div>
<script src="static/js/app.js"></script>
<script>
(async function () {
  var id = new URLSearchParams(location.search).get('id');
  var panel = document.getElementById('event-panel');
  try {
    var data = await App.api('api/events?id=' + encodeURIComponent(id));
    var ev = data.event;
    panel.innerHTML =
      '<h1>' + App.esc(ev.title) + '</h1>'
      + '<p class="muted">Organised by ' + App.esc(ev.organiser)
      + (ev.starts_at ? ' &middot; ' + App.esc(ev.starts_at.replace('T', ' ').substring(0, 16)) + ' UTC' : '')
      + (ev.location ? ' &middot; ' + App.esc(ev.location) : '') + '</p>'
      + '<div class="event-desc">' + ev.description_html + '</div>'
      + (ev.invite_code
          ? '<details class="advanced" open><summary>Volunteer team</summary>'
            + '<p>Join the volunteer team with invite code <span class="code">'
            + App.esc(ev.invite_code) + '</span> (see My Campus once logged in).</p></details>'
          : '');
    document.title = ev.title + ' - Campus Events';
  } catch (e) {
    panel.innerHTML = '<p class="muted">' + App.esc(e.message) + '</p>';
  }
})();
</script>
<%@ include file="WEB-INF/jsp/footer.jspf" %>

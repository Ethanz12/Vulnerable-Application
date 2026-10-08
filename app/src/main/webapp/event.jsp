<%@ include file="WEB-INF/jsp/header.jspf" %>
<div id="event-panel" class="panel"><p class="muted">Loading event...</p></div>
<script src="static/js/app.js"></script>
<script>
(async function () {
  var id = new URLSearchParams(location.search).get('id');
  var panel = document.getElementById('event-panel');
  try {
    var data = await App.api('api/events?id=' + encodeURIComponent(id));
    var ev = data.event;
    var meta = [];
    if (ev.organiser) meta.push('Organised by ' + App.esc(ev.organiser));
    if (ev.starts_at) meta.push(App.esc(ev.starts_at.replace('T', ' ').substring(0, 16)) + ' UTC');
    if (ev.location) meta.push(App.esc(ev.location));

    panel.innerHTML =
      '<h1 style="margin-bottom:0.25rem">' + App.esc(ev.title) + '</h1>'
      + '<p class="muted" style="margin-bottom:1.25rem">' + meta.join(' &middot; ') + '</p>'
      + '<div class="event-desc">' + ev.description_html + '</div>'
      + (ev.invite_code
          ? '<details class="advanced" open><summary>Volunteer team</summary>'
            + '<p class="muted">Join the team with invite code <span class="code">'
            + App.esc(ev.invite_code) + '</span> (see My Campus once logged in).</p></details>'
          : '');
    document.title = ev.title + ' — Campus Events';
  } catch (e) {
    panel.innerHTML = '<div class="empty-state"><p>' + App.esc(e.message) + '</p></div>';
  }
})();
</script>
<%@ include file="WEB-INF/jsp/footer.jspf" %>

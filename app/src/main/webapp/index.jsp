<%@ include file="WEB-INF/jsp/header.jspf" %>
<h1>Upcoming events</h1>
<p class="muted" style="margin-bottom:1.5rem">Discover published campus events. Log in to register attendance or join a volunteer team.</p>
<div id="events" class="cards"><p class="muted">Loading events...</p></div>
<script src="static/js/app.js"></script>
<script>
(async function () {
  var wrap = document.getElementById('events');
  try {
    var data = await App.api('api/events');
    if (!data.events.length) {
      wrap.innerHTML = '<div class="empty-state"><p>No published events yet. Check back soon!</p></div>';
      return;
    }
    wrap.innerHTML = data.events.map(function (ev) {
      var meta = [];
      if (ev.organiser) meta.push(App.esc(ev.organiser));
      if (ev.starts_at) meta.push(App.esc(ev.starts_at.replace('T', ' ').substring(0, 16)) + ' UTC');
      if (ev.location) meta.push(App.esc(ev.location));
      return '<div class="card">'
        + '<h3>' + App.esc(ev.title) + '</h3>'
        + '<p class="muted">' + meta.join(' &middot; ') + '</p>'
        + '<a class="btn" href="event.jsp?id=' + ev.id + '">View event</a>'
        + '</div>';
    }).join('');
  } catch (e) {
    wrap.innerHTML = '<div class="empty-state"><p>Could not load events: ' + App.esc(e.message) + '</p></div>';
  }
})();
</script>
<%@ include file="WEB-INF/jsp/footer.jspf" %>

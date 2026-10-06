<%@ include file="WEB-INF/jsp/header.jspf" %>
<h1>Upcoming events</h1>
<p class="muted">Published campus events. Log in to register attendance or join a volunteer team.</p>
<div id="events" class="cards"><p class="muted">Loading...</p></div>
<%-- page script appended below --%>
<script src="static/js/app.js"></script>
<script>
(async function () {
  var wrap = document.getElementById('events');
  try {
    var data = await App.api('api/events');
    if (!data.events.length) {
      wrap.innerHTML = '<p class="muted">No published events yet.</p>';
      return;
    }
    wrap.innerHTML = data.events.map(function (ev) {
      return '<div class="card">'
        + '<h3>' + App.esc(ev.title) + '</h3>'
        + '<p class="muted">' + App.esc(ev.organiser) + (ev.starts_at ? ' &middot; ' + App.esc(ev.starts_at.replace('T', ' ').substring(0, 16)) + ' UTC' : '') + (ev.location ? ' &middot; ' + App.esc(ev.location) : '') + '</p>'
        + '<a class="btn" href="event.jsp?id=' + ev.id + '">View event</a>'
        + '</div>';
    }).join('');
  } catch (e) {
    wrap.innerHTML = '<p class="muted">Could not load events: ' + App.esc(e.message) + '</p>';
  }
})();
</script>
<%@ include file="WEB-INF/jsp/footer.jspf" %>

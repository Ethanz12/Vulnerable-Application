<%
    campus.web.User pageUser = (campus.web.User) session.getAttribute("user");
    if (pageUser == null) { response.sendRedirect("login.jsp"); return; }
    if (!pageUser.isAdmin()) { response.sendRedirect("index.jsp"); return; }
%>
<%@ include file="WEB-INF/jsp/header.jspf" %>
<h1>Event review</h1>
<div class="panel" id="review-panel"><p class="muted">Loading...</p></div>
<script src="static/js/app.js"></script>
<script>
(async function () {
  var id = new URLSearchParams(location.search).get('id');
  var panel = document.getElementById('review-panel');
  try {
    var data = await App.api('api/admin/review?id=' + encodeURIComponent(id));
    var ev = data.event;
    // Administrator review renders the submitted rich text as-is.
    panel.innerHTML =
      '<h2 style="margin-top:0">' + App.esc(ev.title) + '</h2>'
      + '<p class="muted">Event #' + ev.id + ' &middot; by ' + App.esc(ev.organiser)
      + ' &middot; status ' + App.esc(ev.status) + '</p>'
      + '<div id="review-desc" class="event-desc">' + ev.description_html + '</div>'
      + (ev.status === 'pending_review'
          ? '<button id="approve-btn">Approve and publish</button> '
            + '<button id="reject-btn" class="danger">Reject</button>'
          : '');
    document.title = 'Review: ' + ev.title + ' - Campus Events';
    var approve = document.getElementById('approve-btn');
    var reject = document.getElementById('reject-btn');
    if (approve) {
      approve.addEventListener('click', function () { decide('approve'); });
      reject.addEventListener('click', function () { decide('reject'); });
    }
  } catch (e) {
    panel.innerHTML = '<p class="muted">' + App.esc(e.message) + '</p>';
  }

  async function decide(action) {
    try {
      await App.api('api/admin/review', { action: action, event_id: parseInt(id, 10) });
      App.toast('Event ' + action + 'd');
      setTimeout(function () { location.href = 'admin.jsp'; }, 800);
    } catch (err) { App.toast(err.message, true); }
  }
})();
</script>
<%@ include file="WEB-INF/jsp/footer.jspf" %>

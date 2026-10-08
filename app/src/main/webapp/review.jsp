<%
    campus.web.User pageUser = (campus.web.User) session.getAttribute("user");
    if (pageUser == null) { response.sendRedirect("login.jsp"); return; }
    if (!pageUser.isAdmin()) { response.sendRedirect("index.jsp"); return; }
%>
<%@ include file="WEB-INF/jsp/header.jspf" %>
<div style="margin-bottom:1rem">
  <a href="admin.jsp" style="font-size:0.85rem;color:var(--text-secondary)">&larr; Back to administration</a>
</div>
<div class="panel" id="review-panel"><p class="muted">Loading event...</p></div>
<script src="static/js/app.js"></script>
<script>
(async function () {
  var id = new URLSearchParams(location.search).get('id');
  var panel = document.getElementById('review-panel');
  try {
    var data = await App.api('api/admin/review?id=' + encodeURIComponent(id));
    var ev = data.event;
    panel.innerHTML =
      '<div style="display:flex;align-items:center;gap:0.75rem;margin-bottom:0.5rem">'
      + '<h1 style="margin:0">' + App.esc(ev.title) + '</h1>'
      + '<span class="chip ' + App.esc(ev.status) + '">' + App.esc(ev.status.replace('_', ' ')) + '</span>'
      + '</div>'
      + '<p class="muted" style="margin-bottom:1.25rem">Event #' + ev.id + ' &middot; by ' + App.esc(ev.organiser) + '</p>'
      + '<div id="review-desc" class="event-desc" style="border-top:1px solid var(--border);padding-top:1rem">' + ev.description_html + '</div>'
      + (ev.status === 'pending_review'
          ? '<div style="display:flex;gap:0.5rem;margin-top:1.5rem;padding-top:1rem;border-top:1px solid var(--border)">'
            + '<button id="approve-btn">Approve &amp; publish</button>'
            + '<button id="reject-btn" class="danger">Reject</button>'
            + '</div>'
          : '');
    document.title = 'Review: ' + ev.title + ' — Campus Events';
    var approve = document.getElementById('approve-btn');
    var reject = document.getElementById('reject-btn');
    if (approve) {
      approve.addEventListener('click', function () { decide('approve'); });
      reject.addEventListener('click', function () { decide('reject'); });
    }
  } catch (e) {
    panel.innerHTML = '<div class="empty-state"><p>' + App.esc(e.message) + '</p></div>';
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

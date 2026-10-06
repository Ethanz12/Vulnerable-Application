<%@ include file="WEB-INF/jsp/header.jspf" %>
<div class="panel" style="max-width:26rem">
  <h1>Student sign-up</h1>
  <form id="signup-form">
    <label for="su-username">Username</label>
    <input type="text" id="su-username" required>
    <label for="su-student-id">Student ID</label>
    <input type="text" id="su-student-id" placeholder="S-1234" required>
    <details class="advanced">
      <summary>Advanced</summary>
      <label for="su-email">Email (optional)</label>
      <input type="email" id="su-email">
    </details>
    <button type="submit" id="signup-btn">Register</button>
  </form>
  <p class="muted form-note">Registrations are reviewed at the orientation desk and must be
    activated before first login. See the <a href="activate.jsp">activation page</a> and the
    pending-registration directory.</p>
</div>
<script src="static/js/app.js"></script>
<script>
document.getElementById('signup-form').addEventListener('submit', async function (e) {
  e.preventDefault();
  try {
    var data = await App.api('api/auth', {
      action: 'signup',
      username: document.getElementById('su-username').value,
      student_id: document.getElementById('su-student-id').value,
      email: document.getElementById('su-email').value || ''
    });
    App.toast(data.message);
    setTimeout(function () { location.href = 'activate.jsp'; }, 1200);
  } catch (err) {
    App.toast(err.message, true);
  }
});
</script>
<%@ include file="WEB-INF/jsp/footer.jspf" %>

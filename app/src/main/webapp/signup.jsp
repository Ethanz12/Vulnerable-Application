<%@ include file="WEB-INF/jsp/header.jspf" %>
<div class="auth-container">
  <div class="panel">
    <h1>Create account</h1>
    <p class="muted" style="margin-bottom:1.25rem">Register as a new student on campus</p>
    <form id="signup-form">
      <label for="su-username">Username</label>
      <input type="text" id="su-username" placeholder="Choose a username" required>
      <label for="su-student-id">Student ID</label>
      <input type="text" id="su-student-id" placeholder="e.g. S-1234" required>
      <details class="advanced">
        <summary>Advanced options</summary>
        <label for="su-email">Email <span class="muted">(optional)</span></label>
        <input type="email" id="su-email" placeholder="your@email.com">
      </details>
      <button type="submit" id="signup-btn" style="width:100%">Register</button>
    </form>
  </div>
  <div class="panel" style="text-align:center">
    <p class="muted" style="margin:0">Already have an account? <a href="login.jsp">Sign in</a>. Need to activate? Go to <a href="activate.jsp">activation</a>.</p>
  </div>
</div>
<script src="static/js/app.js"></script>
<script>
document.getElementById('signup-form').addEventListener('submit', async function (e) {
  e.preventDefault();
  var btn = document.getElementById('signup-btn');
  btn.disabled = true;
  btn.textContent = 'Registering...';
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
    btn.disabled = false;
    btn.textContent = 'Register';
  }
});
</script>
<%@ include file="WEB-INF/jsp/footer.jspf" %>

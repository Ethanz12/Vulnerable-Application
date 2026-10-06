<%@ include file="WEB-INF/jsp/header.jspf" %>
<div class="panel" style="max-width:26rem">
  <h1>Log in</h1>
  <form id="login-form">
    <label for="username">Username</label>
    <input type="text" id="username" name="username" autocomplete="username" required>
    <label for="password">Password</label>
    <input type="password" id="password" name="password" autocomplete="current-password" required>
    <button type="submit" id="login-btn">Log in</button>
  </form>
  <p class="muted form-note">No account yet? <a href="signup.jsp">Sign up</a> or
    <a href="activate.jsp">activate</a> a pending registration.</p>
</div>
<script src="static/js/app.js"></script>
<script>
document.getElementById('login-form').addEventListener('submit', async function (e) {
  e.preventDefault();
  try {
    var data = await App.api('api/auth', {
      action: 'login',
      username: document.getElementById('username').value,
      password: document.getElementById('password').value
    });
    location.href = data.redirect;
  } catch (err) {
    App.toast(err.message, true);
  }
});
</script>
<%@ include file="WEB-INF/jsp/footer.jspf" %>

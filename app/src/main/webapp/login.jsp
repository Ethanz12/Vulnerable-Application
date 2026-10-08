<%@ include file="WEB-INF/jsp/header.jspf" %>
<div class="auth-container">
  <div class="panel">
    <h1>Welcome back</h1>
    <p class="muted" style="margin-bottom:1.25rem">Sign in to your campus account</p>
    <form id="login-form">
      <label for="username">Username</label>
      <input type="text" id="username" name="username" autocomplete="username" placeholder="Enter your username" required>
      <label for="password">Password</label>
      <input type="password" id="password" name="password" autocomplete="current-password" placeholder="Enter your password" required>
      <button type="submit" id="login-btn" style="width:100%">Sign in</button>
    </form>
  </div>
  <div class="panel" style="text-align:center">
    <p class="muted" style="margin:0">No account yet? <a href="signup.jsp">Create one</a> or <a href="activate.jsp">activate</a> a pending registration.</p>
  </div>
</div>
<script src="static/js/app.js"></script>
<script>
document.getElementById('login-form').addEventListener('submit', async function (e) {
  e.preventDefault();
  var btn = document.getElementById('login-btn');
  btn.disabled = true;
  btn.textContent = 'Signing in...';
  try {
    var data = await App.api('api/auth', {
      action: 'login',
      username: document.getElementById('username').value,
      password: document.getElementById('password').value
    });
    location.href = data.redirect;
  } catch (err) {
    App.toast(err.message, true);
    btn.disabled = false;
    btn.textContent = 'Sign in';
  }
});
</script>
<%@ include file="WEB-INF/jsp/footer.jspf" %>

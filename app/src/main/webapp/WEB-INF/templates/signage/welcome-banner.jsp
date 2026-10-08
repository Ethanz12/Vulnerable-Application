<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="campus.util.HtmlEscape" %>
<%
    String title = request.getParameter("title");
    if (title == null || title.isBlank()) {
        title = "Welcome to Campus";
    }
%>
<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8">
  <title>Digital Signage - Welcome</title>
  <link rel="stylesheet" href="<%= request.getContextPath() %>/static/css/signage.css">
</head>
<body class="signage signage-welcome">
  <main>
    <h1><%= HtmlEscape.escape(title) %></h1>
    <p class="signage-footer">Campus Digital Signage &middot; welcome-banner.jsp</p>
  </main>
</body>
</html>

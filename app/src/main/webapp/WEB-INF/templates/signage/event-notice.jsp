<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="java.sql.*, campus.db.Database, campus.util.HtmlEscape" %>
<%
    String heading = request.getParameter("heading");
    String annParam = request.getParameter("announcement");
    String body = "";
    if (annParam != null && !annParam.isBlank()) {
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT body_html FROM announcement_templates WHERE id=?")) {
            ps.setInt(1, Integer.parseInt(annParam));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    body = rs.getString("body_html");
                }
            }
        } catch (Exception ignored) {
        }
    }
    if (heading == null || heading.isBlank()) {
        heading = "Campus Notice";
    }
    if (body.isBlank()) {
        body = "<p>No announcement selected. Add ?announcement=&lt;id&gt; to the preview URL.</p>";
    }
%>
<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8">
  <title>Digital Signage - Notice</title>
  <link rel="stylesheet" href="<%= request.getContextPath() %>/static/css/signage.css">
</head>
<body class="signage signage-notice">
  <main>
    <h1><%= HtmlEscape.escape(heading) %></h1>
    <div class="signage-body"><%= body %></div>
    <p class="signage-footer">Campus Digital Signage &middot; event-notice.jsp</p>
  </main>
</body>
</html>

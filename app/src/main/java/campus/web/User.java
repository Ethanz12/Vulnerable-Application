package campus.web;

public class User {

    private final int id;
    private final String username;
    private final String email;
    private final String studentId;
    private final String role;
    private final String status;

    public User(int id, String username, String email, String studentId, String role, String status) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.studentId = studentId;
        this.role = role;
        this.status = status;
    }

    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getStudentId() { return studentId; }
    public String getRole() { return role; }
    public String getStatus() { return status; }

    public boolean isAdmin() { return "admin".equals(role); }
    public boolean isOrganiser() { return "organiser".equals(role) || isAdmin(); }
}

-- Passwords are stored as sha256(lowercase(username) || ':' || password), hex encoded.
-- Seeded accounts (lab-only, synthetic):
--   admin       / Admin#2026!    (Administrator)
--   m.organiser / Organiser#1!   (Organiser)
--   a.chen      / Student#1!     (Student, already active)

INSERT INTO users (username, email, student_id, role, status, password_hash, created_at) VALUES
('admin',       'admin@campus.edu',       NULL,      'admin',     'active', encode(sha256(convert_to('admin:Admin#2026!','UTF8')),'hex'),       '2026-08-01 09:00:00+00'),
('m.organiser', 'mira@campus.edu',        'F-2007',  'organiser', 'active', encode(sha256(convert_to('m.organiser:Organiser#1!','UTF8')),'hex'),'2026-08-02 10:15:00+00'),
('a.chen',      'alex.chen@campus.edu',   'S-1031',  'student',   'active', encode(sha256(convert_to('a.chen:Student#1!','UTF8')),'hex'),       '2026-09-10 14:32:11+00');

-- Pending student. Activation token is derived deterministically from the
-- student ID and the registration epoch seconds:
--   token = substring(md5('ACTIVATE:' || student_id || ':' || epoch_seconds), 1, 8)
-- created_at is pinned to 2026-10-05 12:00:00 UTC == epoch 1791201600.
INSERT INTO users (username, email, student_id, role, status, created_at, activation_token) VALUES
('r.patel', 'rahul.patel@campus.edu', 'S-1042', 'student', 'pending', '2026-10-05 12:00:00+00',
  substring(md5('ACTIVATE:S-1042:1791201600'), 1, 8));

INSERT INTO events (title, description_html, status, organiser_id, starts_at, location) VALUES
('Fall Fest 2026',
 '<p>The biggest campus party of the year. Live music, food stalls and the club fair on the main lawn.</p><p>Volunteers get a free T-shirt - join the team with the invite code on this page!</p>',
 'published', 2, '2026-10-24 16:00:00+00', 'Main Lawn'),
('Hackathon Night',
 '<p>12-hour hackathon in the innovation lab. Teams of four, pizza provided.</p><p>Draft programme - awaiting organiser review.</p>',
 'draft', 2, '2026-11-06 18:00:00+00', 'Innovation Lab B2'),
('Career Fair',
 '<p>Meet 40+ employers on campus. Bring your CV.</p>',
 'published', 2, '2026-10-30 10:00:00+00', 'Sports Hall');

-- Public volunteer invite for Fall Fest (shown on the event page).
INSERT INTO invitations (token, event_id, allowed_role, created_by) VALUES
('INV-FALLFEST-2026', 1, 'volunteer', 2);

INSERT INTO event_team (event_id, user_id, role) VALUES (1, 2, 'organiser');

INSERT INTO announcement_templates (name, body_html) VALUES
('Event postponed',
 '<h2>Event postponed</h2><p>{{event}} has been postponed. New date: {{new_date}}. Tickets remain valid.</p>'),
('Volunteers needed',
 '<h2>Volunteers needed</h2><p>We still need helpers for {{event}}. Sign up at the student office.</p>');

INSERT INTO audit_log (actor, action, detail) VALUES
('system', 'lab_init', 'Seeded demonstration data for security training lab');

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
 'published', 2, '2026-10-30 10:00:00+00', 'Sports Hall'),
('Open-Air Cinema Night',
 '<p>Blankets, popcorn and a double bill on the big screen in the North Quad.</p><ul><li>19:30 - Studio classics shorts</li><li>20:15 - Feature film (student vote winner)</li></ul><p>Free entry, no ticket needed.</p>',
 'published', 2, '2026-11-12 19:30:00+00', 'North Quad'),
('Robotics Showcase',
 '<p>Student teams demonstrate their semester robots: line followers, sorters and the annual maze challenge.</p><p>Industry judges award the Innovation Prize at 16:00.</p>',
 'published', 2, '2026-11-21 13:00:00+00', 'Engineering Atrium'),
('Winter Choral Concert',
 '<p>The campus choir and brass ensemble perform a winter programme in the Great Hall.</p><p>Doors open 18:30. Refreshments served during the interval.</p>',
 'published', 2, '2026-12-04 19:00:00+00', 'Great Hall'),
('Sustainability Week Kickoff',
 '<p>Launch event for Sustainability Week: swap market, repair cafe and the green societies fair.</p><p>Volunteers help with stalls and recycling points - join the team with the invite code on this page.</p>',
 'published', 2, '2026-12-09 11:00:00+00', 'Main Lawn'),
('Alumni Networking Evening',
 '<p>Informal networking with returning alumni from industry and research.</p><p>Draft guest list still being finalised.</p>',
 'draft', 2, '2026-12-18 18:00:00+00', 'Business School Lounge'),
('Autumn Photography Walk',
 '<p>A slow loop around the oldest parts of campus with the photography society. All cameras welcome - phones count.</p><ul><li>Meet at the library steps</li><li>Two-hour loop, ends at the campus cafe</li><li>Best shots featured on the society noticeboard</li></ul>',
 'published', 2, '2026-11-15 14:00:00+00', 'Library Steps'),
('Inter-University Football Final',
 '<p>Our team hosts Westgate University in the season final. Admission free with student ID.</p><p>Gates open one hour before kick-off; the north stand is the student section.</p>',
 'published', 2, '2026-11-28 15:00:00+00', 'Sports Arena'),
('Guest Lecture: Machine Learning in Medicine',
 '<p>Dr Amara Okafor (St Mary''s Research Centre) on how diagnostic models are trained, validated and deployed in hospitals.</p><blockquote><p>"The hardest part is not the model - it is everything around the model."</p></blockquote><p>Followed by Q&amp;A and refreshments.</p>',
 'published', 2, '2026-12-01 17:00:00+00', 'Lecture Hall B'),
('Charity Bake-Off',
 '<p>Student teams sell baked goods for the campus food bank. Buy a token at the door and vote for your favourite stall.</p><ul><li>Cakes and tray bakes</li><li>Bread and savouries</li><li>Free-from category</li></ul><p>All proceeds matched by the Student Union.</p>',
 'published', 2, '2026-12-06 12:00:00+00', 'Student Union Kitchen'),
('Game Jam Showcase',
 '<p>Twelve teams, forty-eight hours, one theme. Play the prototypes, meet the jam teams and vote for the audience award.</p><p>The media suite labs are open to visitors all afternoon.</p>',
 'published', 2, '2026-12-12 16:00:00+00', 'Media Suite'),
('Astronomy Night: The Winter Sky',
 '<p>The physics society sets up telescopes on the observatory deck. Wrapped-up viewing of the Pleiades, Jupiter and whatever the clouds allow.</p><table><tr><th>Time</th><th>Programme</th></tr><tr><td>20:00</td><td>Introduction and sky orientation</td></tr><tr><td>20:30</td><td>Telescope stations</td></tr><tr><td>22:00</td><td>Late session: astrophotography basics</td></tr></table>',
 'published', 2, '2026-12-15 20:00:00+00', 'Observatory Deck');

-- Public volunteer invites for published events (shown on the event pages).
INSERT INTO invitations (token, event_id, allowed_role, created_by) VALUES
('INV-FALLFEST-2026', 1, 'volunteer', 2),
('INV-CINEMA-2026', 4, 'volunteer', 2),
('INV-SUSTAIN-2026', 7, 'volunteer', 2);

INSERT INTO event_team (event_id, user_id, role) VALUES (1, 2, 'organiser');

INSERT INTO announcement_templates (name, body_html) VALUES
('Event postponed',
 '<h2>Event postponed</h2><p>{{event}} has been postponed. New date: {{new_date}}. Tickets remain valid.</p>'),
('Volunteers needed',
 '<h2>Volunteers needed</h2><p>We still need helpers for {{event}}. Sign up at the student office.</p>'),
('Road closure notice',
 '<h2>Campus access notice</h2><p>Due to {{event}}, the road between the library and the sports hall is closed from {{start_time}}. Please use the north entrance.</p>');

INSERT INTO audit_log (actor, action, detail) VALUES
('system', 'lab_init', 'Seeded demonstration data for security training lab');

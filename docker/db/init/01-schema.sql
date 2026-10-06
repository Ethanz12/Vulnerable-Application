CREATE TABLE users (
    id                SERIAL PRIMARY KEY,
    username          TEXT UNIQUE NOT NULL,
    email             TEXT,
    student_id        TEXT,
    role              TEXT NOT NULL CHECK (role IN ('student','organiser','admin')),
    status            TEXT NOT NULL DEFAULT 'pending' CHECK (status IN ('pending','active')),
    password_hash     TEXT,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    activation_token  TEXT
);

CREATE TABLE events (
    id               SERIAL PRIMARY KEY,
    title            TEXT NOT NULL,
    description_html TEXT NOT NULL DEFAULT '',
    status           TEXT NOT NULL DEFAULT 'draft'
                     CHECK (status IN ('draft','pending_review','published','rejected')),
    organiser_id     INT REFERENCES users(id),
    starts_at        TIMESTAMPTZ,
    location         TEXT,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE event_team (
    id        SERIAL PRIMARY KEY,
    event_id  INT NOT NULL REFERENCES events(id),
    user_id   INT NOT NULL REFERENCES users(id),
    role      TEXT NOT NULL,
    joined_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (event_id, user_id, role)
);

CREATE TABLE invitations (
    token        TEXT PRIMARY KEY,
    event_id     INT NOT NULL REFERENCES events(id),
    allowed_role TEXT NOT NULL DEFAULT 'volunteer',
    created_by   INT REFERENCES users(id)
);

CREATE TABLE attendance (
    id         SERIAL PRIMARY KEY,
    event_id   INT NOT NULL REFERENCES events(id),
    user_id    INT NOT NULL REFERENCES users(id),
    marked_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (event_id, user_id)
);

CREATE TABLE announcement_templates (
    id         SERIAL PRIMARY KEY,
    name       TEXT NOT NULL,
    body_html  TEXT NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE audit_log (
    id     SERIAL PRIMARY KEY,
    actor  TEXT,
    action TEXT NOT NULL,
    detail TEXT,
    at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

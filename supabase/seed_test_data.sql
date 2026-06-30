-- =============================================================================
-- MUST-CONNECT: Supabase Schema + Test Seed Script
-- =============================================================================
-- Roles: 1 Super Admin, 1 Dept Admin, 2 Teachers, 2 Students
--
-- Email convention (from supabase_rules.md):
--   Super Admin  → superadmin@must.edu.pk
--   Dept Admin   → deptadmin@must.edu.pk
--   Teachers     → <username>@must.edu.pk
--   Students     → <rollnumber>@must.edu.pk
--
-- INSTRUCTIONS:
--   1. Run SECTION A in Supabase SQL Editor to create the schema + RLS.
--   2. Run SECTION B statements ONE AT A TIME to create Auth users.
--      Copy each returned UUID for use in Section D.
--   3. Run SECTION D (after replacing placeholder UUIDs) to seed profiles.
--   4. Run SECTION E to verify row counts.
-- =============================================================================


-- =============================================================================
-- SECTION A: TABLE DEFINITIONS + ROW LEVEL SECURITY
-- =============================================================================

-- ── super_admin_profiles ──────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.super_admin_profiles (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    full_name  TEXT NOT NULL,
    username   TEXT NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
ALTER TABLE public.super_admin_profiles ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Super admins can read their own profile"
    ON public.super_admin_profiles FOR SELECT
    USING (auth.uid() = user_id);

-- ── dept_admin_profiles ───────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.dept_admin_profiles (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    full_name  TEXT NOT NULL,
    username   TEXT NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
ALTER TABLE public.dept_admin_profiles ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Dept admins can read their own profile"
    ON public.dept_admin_profiles FOR SELECT
    USING (auth.uid() = user_id);

-- ── teacher_profiles ──────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.teacher_profiles (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    full_name  TEXT NOT NULL,
    username   TEXT NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
ALTER TABLE public.teacher_profiles ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Teachers can read own profile"
    ON public.teacher_profiles FOR SELECT
    USING (auth.uid() = user_id);
CREATE POLICY "Dept admin can read all teacher profiles"
    ON public.teacher_profiles FOR SELECT
    USING (EXISTS (
        SELECT 1 FROM public.dept_admin_profiles WHERE user_id = auth.uid()
    ));
CREATE POLICY "Super admin can read all teacher profiles"
    ON public.teacher_profiles FOR SELECT
    USING (EXISTS (
        SELECT 1 FROM public.super_admin_profiles WHERE user_id = auth.uid()
    ));

-- ── student_profiles ──────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.student_profiles (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    full_name   TEXT NOT NULL,
    roll_number TEXT NOT NULL UNIQUE,
    section     TEXT NOT NULL DEFAULT '',
    semester    INT  NOT NULL DEFAULT 1,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
ALTER TABLE public.student_profiles ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Students can read own profile"
    ON public.student_profiles FOR SELECT
    USING (auth.uid() = user_id);
CREATE POLICY "Teachers and admins can read student profiles"
    ON public.student_profiles FOR SELECT
    USING (
        EXISTS (SELECT 1 FROM public.teacher_profiles    WHERE user_id = auth.uid())
        OR EXISTS (SELECT 1 FROM public.dept_admin_profiles  WHERE user_id = auth.uid())
        OR EXISTS (SELECT 1 FROM public.super_admin_profiles WHERE user_id = auth.uid())
    );

-- ── all_profiles (flat view for Super Admin monitoring) ───────────────────────
CREATE OR REPLACE VIEW public.all_profiles AS
    SELECT id, user_id, full_name, username    AS identifier, 'SUPER_ADMIN' AS role FROM public.super_admin_profiles
    UNION ALL
    SELECT id, user_id, full_name, username    AS identifier, 'DEPT_ADMIN'  AS role FROM public.dept_admin_profiles
    UNION ALL
    SELECT id, user_id, full_name, username    AS identifier, 'TEACHER'     AS role FROM public.teacher_profiles
    UNION ALL
    SELECT id, user_id, full_name, roll_number AS identifier, 'STUDENT'     AS role FROM public.student_profiles;

-- ── class_groups ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.class_groups (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name       TEXT NOT NULL,
    subject    TEXT NOT NULL,
    teacher_id UUID REFERENCES public.teacher_profiles(id) ON DELETE SET NULL,
    created_by UUID NOT NULL REFERENCES public.dept_admin_profiles(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
ALTER TABLE public.class_groups ENABLE ROW LEVEL SECURITY;
CREATE POLICY "All authenticated users can read class groups"
    ON public.class_groups FOR SELECT
    USING (auth.role() = 'authenticated');
CREATE POLICY "Only dept admin can manage class groups"
    ON public.class_groups FOR ALL
    USING (EXISTS (
        SELECT 1 FROM public.dept_admin_profiles WHERE user_id = auth.uid()
    ));

-- ── class_memberships ────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.class_memberships (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    class_id   UUID NOT NULL REFERENCES public.class_groups(id)   ON DELETE CASCADE,
    student_id UUID NOT NULL REFERENCES public.student_profiles(id) ON DELETE CASCADE,
    joined_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(class_id, student_id)
);
ALTER TABLE public.class_memberships ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Students can read own memberships"
    ON public.class_memberships FOR SELECT
    USING (EXISTS (
        SELECT 1 FROM public.student_profiles WHERE id = student_id AND user_id = auth.uid()
    ));
CREATE POLICY "Teachers and admins can read all memberships"
    ON public.class_memberships FOR SELECT
    USING (
        EXISTS (SELECT 1 FROM public.teacher_profiles    WHERE user_id = auth.uid())
        OR EXISTS (SELECT 1 FROM public.dept_admin_profiles  WHERE user_id = auth.uid())
        OR EXISTS (SELECT 1 FROM public.super_admin_profiles WHERE user_id = auth.uid())
    );

-- ── class_posts ───────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.class_posts (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    class_id    UUID NOT NULL REFERENCES public.class_groups(id) ON DELETE CASCADE,
    author_id   UUID NOT NULL,
    author_name TEXT,
    title       TEXT NOT NULL,
    content     TEXT NOT NULL,
    file_url    TEXT,
    is_pinned   BOOLEAN NOT NULL DEFAULT false,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
ALTER TABLE public.class_posts ENABLE ROW LEVEL SECURITY;
CREATE POLICY "All authenticated users can read class posts"
    ON public.class_posts FOR SELECT
    USING (auth.role() = 'authenticated');
CREATE POLICY "Teachers can insert class posts"
    ON public.class_posts FOR INSERT
    WITH CHECK (EXISTS (
        SELECT 1 FROM public.teacher_profiles WHERE user_id = auth.uid()
    ));
CREATE POLICY "Authors can delete own class posts"
    ON public.class_posts FOR DELETE
    USING (auth.uid() = author_id);

-- ── general_feed_posts ────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.general_feed_posts (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    author_id   UUID NOT NULL,
    author_name TEXT,
    title       TEXT NOT NULL,
    content     TEXT NOT NULL,
    is_pinned   BOOLEAN NOT NULL DEFAULT false,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
ALTER TABLE public.general_feed_posts ENABLE ROW LEVEL SECURITY;
CREATE POLICY "All authenticated users can read general feed"
    ON public.general_feed_posts FOR SELECT
    USING (auth.role() = 'authenticated');
CREATE POLICY "Only dept admin can post to general feed"
    ON public.general_feed_posts FOR INSERT
    WITH CHECK (EXISTS (
        SELECT 1 FROM public.dept_admin_profiles WHERE user_id = auth.uid()
    ));
CREATE POLICY "Dept admin and super admin can delete general feed posts"
    ON public.general_feed_posts FOR DELETE
    USING (
        EXISTS (SELECT 1 FROM public.dept_admin_profiles  WHERE user_id = auth.uid())
        OR EXISTS (SELECT 1 FROM public.super_admin_profiles WHERE user_id = auth.uid())
    );

-- ── direct_messages ───────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.direct_messages (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sender_id   UUID NOT NULL,
    receiver_id UUID NOT NULL,
    content     TEXT NOT NULL,
    sent_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    is_read     BOOLEAN NOT NULL DEFAULT false
);
ALTER TABLE public.direct_messages ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Users can read own messages"
    ON public.direct_messages FOR SELECT
    USING (auth.uid() = sender_id OR auth.uid() = receiver_id);
CREATE POLICY "Users can send messages"
    ON public.direct_messages FOR INSERT
    WITH CHECK (auth.uid() = sender_id);
CREATE POLICY "Super admin can monitor all messages"
    ON public.direct_messages FOR SELECT
    USING (EXISTS (
        SELECT 1 FROM public.super_admin_profiles WHERE user_id = auth.uid()
    ));


-- =============================================================================
-- SECTION B: CREATE TEST AUTH USERS (run each INSERT separately)
-- =============================================================================
-- Copy the UUID returned by each statement and replace the placeholders
-- in Section D below.
--
-- Test credentials:
--   Super Admin  → superadmin@must.edu.pk   / SuperAdmin@123
--   Dept Admin   → deptadmin@must.edu.pk    / DeptAdmin@123
--   Teacher 1    → sarah.jenkins@must.edu.pk / Teacher@123
--   Teacher 2    → ali.raza@must.edu.pk      / Teacher@123
--   Student 1    → BSCS-F21-001@must.edu.pk  / Student@123
--   Student 2    → BSCS-F21-002@must.edu.pk  / Student@123

INSERT INTO auth.users (id, email, encrypted_password, email_confirmed_at, raw_user_meta_data, role, aud, created_at, updated_at)
VALUES (gen_random_uuid(), 'superadmin@must.edu.pk', crypt('SuperAdmin@123', gen_salt('bf')), now(), '{"full_name":"System Super Admin"}'::jsonb, 'authenticated', 'authenticated', now(), now())
RETURNING id;  -- SUPER_ADMIN_USER_ID

INSERT INTO auth.users (id, email, encrypted_password, email_confirmed_at, raw_user_meta_data, role, aud, created_at, updated_at)
VALUES (gen_random_uuid(), 'deptadmin@must.edu.pk', crypt('DeptAdmin@123', gen_salt('bf')), now(), '{"full_name":"Dr. Usman Ahmed"}'::jsonb, 'authenticated', 'authenticated', now(), now())
RETURNING id;  -- DEPT_ADMIN_USER_ID

INSERT INTO auth.users (id, email, encrypted_password, email_confirmed_at, raw_user_meta_data, role, aud, created_at, updated_at)
VALUES (gen_random_uuid(), 'sarah.jenkins@must.edu.pk', crypt('Teacher@123', gen_salt('bf')), now(), '{"full_name":"Dr. Sarah Jenkins"}'::jsonb, 'authenticated', 'authenticated', now(), now())
RETURNING id;  -- TEACHER1_USER_ID

INSERT INTO auth.users (id, email, encrypted_password, email_confirmed_at, raw_user_meta_data, role, aud, created_at, updated_at)
VALUES (gen_random_uuid(), 'ali.raza@must.edu.pk', crypt('Teacher@123', gen_salt('bf')), now(), '{"full_name":"Mr. Ali Raza"}'::jsonb, 'authenticated', 'authenticated', now(), now())
RETURNING id;  -- TEACHER2_USER_ID

INSERT INTO auth.users (id, email, encrypted_password, email_confirmed_at, raw_user_meta_data, role, aud, created_at, updated_at)
VALUES (gen_random_uuid(), 'BSCS-F21-001@must.edu.pk', crypt('Student@123', gen_salt('bf')), now(), '{"full_name":"Ahmed Khan"}'::jsonb, 'authenticated', 'authenticated', now(), now())
RETURNING id;  -- STUDENT1_USER_ID

INSERT INTO auth.users (id, email, encrypted_password, email_confirmed_at, raw_user_meta_data, role, aud, created_at, updated_at)
VALUES (gen_random_uuid(), 'BSCS-F21-002@must.edu.pk', crypt('Student@123', gen_salt('bf')), now(), '{"full_name":"Fatima Malik"}'::jsonb, 'authenticated', 'authenticated', now(), now())
RETURNING id;  -- STUDENT2_USER_ID


-- =============================================================================
-- SECTION D: SEED PROFILE ROWS + SAMPLE DATA
-- =============================================================================
-- Replace each <PLACEHOLDER> with the UUID returned from Section B above.

DO $$
DECLARE
    super_admin_uid  UUID := '<SUPER_ADMIN_USER_ID>';
    dept_admin_uid   UUID := '<DEPT_ADMIN_USER_ID>';
    teacher1_uid     UUID := '<TEACHER1_USER_ID>';
    teacher2_uid     UUID := '<TEACHER2_USER_ID>';
    student1_uid     UUID := '<STUDENT1_USER_ID>';
    student2_uid     UUID := '<STUDENT2_USER_ID>';

    dept_admin_pid   UUID;
    teacher1_pid     UUID;
    student1_pid     UUID;
    student2_pid     UUID;
    class1_id        UUID;
BEGIN

    -- Profiles
    INSERT INTO public.super_admin_profiles (user_id, full_name, username)
    VALUES (super_admin_uid, 'System Super Admin', 'superadmin');

    INSERT INTO public.dept_admin_profiles (user_id, full_name, username)
    VALUES (dept_admin_uid, 'Dr. Usman Ahmed', 'deptadmin')
    RETURNING id INTO dept_admin_pid;

    INSERT INTO public.teacher_profiles (user_id, full_name, username)
    VALUES (teacher1_uid, 'Dr. Sarah Jenkins', 'sarah.jenkins')
    RETURNING id INTO teacher1_pid;

    INSERT INTO public.teacher_profiles (user_id, full_name, username)
    VALUES (teacher2_uid, 'Mr. Ali Raza', 'ali.raza');

    INSERT INTO public.student_profiles (user_id, full_name, roll_number, section, semester)
    VALUES (student1_uid, 'Ahmed Khan', 'BSCS-F21-001', 'A', 7)
    RETURNING id INTO student1_pid;

    INSERT INTO public.student_profiles (user_id, full_name, roll_number, section, semester)
    VALUES (student2_uid, 'Fatima Malik', 'BSCS-F21-002', 'A', 7)
    RETURNING id INTO student2_pid;

    -- Sample class
    INSERT INTO public.class_groups (name, subject, teacher_id, created_by)
    VALUES ('BSCS-7A – Data Structures', 'Data Structures', teacher1_pid, dept_admin_pid)
    RETURNING id INTO class1_id;

    -- Enrol students
    INSERT INTO public.class_memberships (class_id, student_id) VALUES (class1_id, student1_pid);
    INSERT INTO public.class_memberships (class_id, student_id) VALUES (class1_id, student2_pid);

    -- General feed welcome post
    INSERT INTO public.general_feed_posts (author_id, author_name, title, content, is_pinned)
    VALUES (
        dept_admin_uid,
        'Dr. Usman Ahmed',
        'Welcome to MUST-CONNECT',
        'This is the official communication platform for the CS & IT Department. All announcements, timetables, and academic updates will be shared here. Please check regularly.',
        true
    );

    -- Sample class post
    INSERT INTO public.class_posts (class_id, author_id, author_name, title, content, is_pinned)
    VALUES (
        class1_id,
        teacher1_uid,
        'Dr. Sarah Jenkins',
        'Week 1 – Introduction to Data Structures',
        'Please review Chapter 1 of Cormen et al. before our next session. Assignment 1 will be uploaded by Friday.',
        false
    );

END $$;


-- =============================================================================
-- SECTION E: VERIFY ROW COUNTS
-- =============================================================================
SELECT 'super_admin_profiles' AS tbl, COUNT(*) FROM public.super_admin_profiles
UNION ALL SELECT 'dept_admin_profiles',  COUNT(*) FROM public.dept_admin_profiles
UNION ALL SELECT 'teacher_profiles',     COUNT(*) FROM public.teacher_profiles
UNION ALL SELECT 'student_profiles',     COUNT(*) FROM public.student_profiles
UNION ALL SELECT 'class_groups',         COUNT(*) FROM public.class_groups
UNION ALL SELECT 'class_memberships',    COUNT(*) FROM public.class_memberships
UNION ALL SELECT 'general_feed_posts',   COUNT(*) FROM public.general_feed_posts
UNION ALL SELECT 'class_posts',          COUNT(*) FROM public.class_posts;

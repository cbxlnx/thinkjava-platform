-- database/seed/01_seed_lessons.sql
INSERT INTO lessons (title, content, topic, difficulty, prerequisites)
VALUES
('Java Variables – Basics', 'Intro to types, declarations, assignment, naming.', 'Variables', 'Beginner', ARRAY[]::TEXT[]),
('Control Flow – Loops I', 'for/while basics, counters, off‑by‑one.', 'Loops', 'Beginner', ARRAY['Variables']),
('Arrays 101', 'Declaring arrays, indexing, iteration patterns.', 'Arrays', 'Beginner', ARRAY['Variables','Loops']),
('Methods & Parameters', 'Defining methods, parameters, return values.', 'Methods', 'Intermediate', ARRAY['Variables','Arrays']),
('OOP Foundations', 'Classes, objects, fields, methods, constructors.', 'OOP', 'Intermediate', ARRAY['Methods']);
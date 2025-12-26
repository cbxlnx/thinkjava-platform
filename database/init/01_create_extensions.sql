-- database/init/01_create_extensions.sql
CREATE EXTENSION IF NOT EXISTS vector;


-- Tables 
CREATE TABLE IF NOT EXISTS users (
id SERIAL PRIMARY KEY,
email TEXT UNIQUE NOT NULL,
password TEXT NOT NULL,
created_at TIMESTAMP NOT NULL DEFAULT NOW()
);


CREATE TABLE IF NOT EXISTS lessons (
id SERIAL PRIMARY KEY,
title TEXT NOT NULL,
content TEXT NOT NULL,
topic TEXT NOT NULL,
difficulty TEXT NOT NULL,
prerequisites TEXT[],
-- future RAG: optional embedding column
embedding vector(384)
);


CREATE TABLE IF NOT EXISTS user_progress (
id SERIAL PRIMARY KEY,
user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
topic TEXT NOT NULL,
mastery_score NUMERIC(3,2) NOT NULL DEFAULT 0.00,
last_updated TIMESTAMP NOT NULL DEFAULT NOW(),
UNIQUE(user_id, topic)
);
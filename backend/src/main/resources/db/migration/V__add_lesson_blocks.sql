create table lesson_block (
  id uuid primary key,
  lesson_id uuid not null references lesson(id),
  order_index int not null,
  type varchar(30) not null,
  markdown text,
  video_title varchar(255),
  video_url varchar(1024)
);

create table inline_quiz_question (
  id uuid primary key,
  block_id uuid not null references lesson_block(id),
  prompt text not null,
  options_json text not null,
  correct_option varchar(255) not null,
  explanation text
);

create table lesson_block_progress (
  id uuid primary key,
  user_id uuid not null references users(id),
  lesson_id uuid not null,
  block_id uuid not null,
  completed boolean not null,
  completed_at timestamp,
  constraint uq_user_block unique (user_id, block_id)
);
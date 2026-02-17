package com.thinkjava.platform.learn.progress;

import com.thinkjava.platform.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LessonProgressRepository extends JpaRepository<LessonProgress, UUID> {

  Optional<LessonProgress> findByUserAndLessonId(User user, UUID lessonId);

  List<LessonProgress> findByUser(User user);
}
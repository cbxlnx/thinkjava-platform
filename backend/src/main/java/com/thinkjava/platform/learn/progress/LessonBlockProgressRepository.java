package com.thinkjava.platform.learn.progress;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LessonBlockProgressRepository extends JpaRepository<LessonBlockProgress, UUID> {
  Optional<LessonBlockProgress> findByUserIdAndBlockId(Long userId, UUID blockId);
  List<LessonBlockProgress> findByUserIdAndLessonId(Long userId, UUID lessonId);
}
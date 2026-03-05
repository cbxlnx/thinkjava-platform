package com.thinkjava.platform.learn.section;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface LessonBlockRepository extends JpaRepository<LessonBlock, UUID> {
  List<LessonBlock> findByLessonIdOrderByOrderIndexAsc(UUID lessonId);
}
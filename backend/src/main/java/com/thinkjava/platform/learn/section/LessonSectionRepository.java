package com.thinkjava.platform.learn.section;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LessonSectionRepository extends JpaRepository<LessonSection, UUID> {

  List<LessonSection> findByLessonIdOrderBySectionOrderAsc(UUID lessonId);
}
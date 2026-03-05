package com.thinkjava.platform.learn.section;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface InlineQuizQuestionRepository extends JpaRepository<InlineQuizQuestion, UUID> {
  List<InlineQuizQuestion> findByBlockIdOrderByIdAsc(UUID blockId);
}
package com.thinkjava.platform.learn.section;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface LessonBlockRepository extends JpaRepository<LessonBlock, UUID> {

  List<LessonBlock> findByLessonIdOrderByOrderIndexAsc(UUID lessonId);

  @Modifying
  @Query(value = """
      update lesson_section
      set embedding_text = :embeddingText,
          embedding = cast(:embedding as vector)
      where id = :id
      """, nativeQuery = true)
  void updateEmbedding(
      @Param("id") UUID id,
      @Param("embeddingText") String embeddingText,
      @Param("embedding") String embedding);

  @Query(value = """
      select
          id as id,
          lesson_id as lessonId,
          section_order as sectionOrder,
          type as type,
          1 - (embedding <=> cast(:queryEmbedding as vector)) as similarity
      from lesson_section
      where lesson_id = :lessonId
        and embedding is not null
      order by embedding <=> cast(:queryEmbedding as vector)
      limit :limit
      """, nativeQuery = true)
  List<LessonBlockSearchResult> findTopSimilarInLesson(
      @Param("lessonId") UUID lessonId,
      @Param("queryEmbedding") String queryEmbedding,
      @Param("limit") int limit);

  @Query(value = """
      select
          id as id,
          lesson_id as lessonId,
          section_order as sectionOrder,
          type as type,
          1 - (embedding <=> cast(:queryEmbedding as vector)) as similarity
      from lesson_section
      where embedding is not null
      order by embedding <=> cast(:queryEmbedding as vector)
      limit :limit
      """, nativeQuery = true)
  List<LessonBlockSearchResult> findTopSimilarAcrossAllLessons(
      @Param("queryEmbedding") String queryEmbedding,
      @Param("limit") int limit);
}
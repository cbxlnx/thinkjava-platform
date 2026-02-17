package com.thinkjava.platform.learn.lesson;

import com.thinkjava.platform.learn.model.Checkpoint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LessonRepository extends JpaRepository<Lesson, UUID> {

  List<Lesson> findByActiveTrueOrderByCheckpointAscOrderIndexAsc();

  List<Lesson> findByCheckpointAndActiveTrueOrderByOrderIndexAsc(Checkpoint checkpoint);
}
package com.thinkjava.platform.learn.section;

import com.thinkjava.platform.learn.lesson.Lesson;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "lesson_section")
public class LessonSection {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "lesson_id", nullable = false)
  private Lesson lesson;

  @Column(name = "section_order", nullable = false)
  private int sectionOrder;

  @Column(nullable = false, columnDefinition = "text")
  private String markdown;

  // ---- getters/setters ----

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public Lesson getLesson() {
    return lesson;
  }

  public void setLesson(Lesson lesson) {
    this.lesson = lesson;
  }

  public int getSectionOrder() {
    return sectionOrder;
  }

  public void setSectionOrder(int sectionOrder) {
    this.sectionOrder = sectionOrder;
  }

  public String getMarkdown() {
    return markdown;
  }

  public void setMarkdown(String markdown) {
    this.markdown = markdown;
  }
}
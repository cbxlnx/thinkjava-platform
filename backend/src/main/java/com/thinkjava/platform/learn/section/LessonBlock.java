package com.thinkjava.platform.learn.section;

import com.thinkjava.platform.learn.lesson.Lesson;
import com.thinkjava.platform.learn.model.LessonBlockType;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "lesson_section")
public class LessonBlock {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "lesson_id", nullable = false)
  private Lesson lesson;

  @Column(name = "section_order", nullable = false)
  private int orderIndex;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private LessonBlockType type = LessonBlockType.MARKDOWN; // default for existing rows

  // MARKDOWN
  @Column(columnDefinition = "text")
  private String markdown;

  // VIDEO (placeholder or real URL)
  @Column(name = "video_title")
  private String videoTitle;

  @Column(name = "video_url")
  private String videoUrl;

  @Column(columnDefinition = "jsonb")
  private  String payload;

  public LessonBlock() {
  }

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

  public int getOrderIndex() {
    return orderIndex;
  }

  public void setOrderIndex(int orderIndex) {
    this.orderIndex = orderIndex;
  }

  public LessonBlockType getType() {
    return type;
  }

  public void setType(LessonBlockType type) {
    this.type = type;
  }

  public String getMarkdown() {
    return markdown;
  }

  public void setMarkdown(String markdown) {
    this.markdown = markdown;
  }

  public String getVideoTitle() {
    return videoTitle;
  }

  public void setVideoTitle(String videoTitle) {
    this.videoTitle = videoTitle;
  }

  public String getVideoUrl() {
    return videoUrl;
  }

  public void setVideoUrl(String videoUrl) {
    this.videoUrl = videoUrl;
  }

  public  String getPayload() {
    return payload;
  }

  public void setPayload( String payload) {
    this.payload = payload;
  }
}
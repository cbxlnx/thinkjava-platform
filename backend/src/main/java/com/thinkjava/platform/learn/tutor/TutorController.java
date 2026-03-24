package com.thinkjava.platform.learn.tutor;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tutor")
public class TutorController {

    private final TutorService tutorService;

    public TutorController(TutorService tutorService) {
        this.tutorService = tutorService;
    }

    @PostMapping("/ask")
    public TutorAskResponse ask(@RequestBody TutorAskRequest request) {
        return tutorService.searchRelevantSections(request.lessonId(), request.question());
    }
}
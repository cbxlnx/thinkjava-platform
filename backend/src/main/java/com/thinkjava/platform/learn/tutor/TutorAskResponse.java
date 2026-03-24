package com.thinkjava.platform.learn.tutor;

import java.util.List;

public record TutorAskResponse(
        String question,
        String answer,
        List<TutorSearchResultDto> matches
) {}
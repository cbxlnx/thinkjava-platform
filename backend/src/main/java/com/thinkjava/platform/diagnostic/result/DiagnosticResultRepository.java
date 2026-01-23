package com.thinkjava.platform.diagnostic.result;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

//database gateway that stores and retrieves each user’s diagnostic mastery profile
public interface DiagnosticResultRepository extends JpaRepository<DiagnosticResult, Long> {
  Optional<DiagnosticResult> findByUserId(Long userId);
}
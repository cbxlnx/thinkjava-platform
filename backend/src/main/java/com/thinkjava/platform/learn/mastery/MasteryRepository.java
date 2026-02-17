package com.thinkjava.platform.learn.mastery;

import com.thinkjava.platform.learn.model.Checkpoint;
import com.thinkjava.platform.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MasteryRepository extends JpaRepository<Mastery, UUID> {

  List<Mastery> findByUser(User user);

  Optional<Mastery> findByUserAndCheckpoint(User user, Checkpoint checkpoint);
}
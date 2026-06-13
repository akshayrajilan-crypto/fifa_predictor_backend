package com.fifaworldcup.Fifa.repository;

import com.fifaworldcup.Fifa.model.InviteCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InviteCodeRepository extends JpaRepository<InviteCode, Long> {
    Optional<InviteCode> findByCode(String code);
    List<InviteCode> findAllByOrderByCreatedAtDesc();
    List<InviteCode> findByUsed(boolean used);
}

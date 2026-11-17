package com.swayam.rewards.repository;

import com.swayam.rewards.model.Member;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    boolean existsByEmail(String email);

    /**
     * Loads a member with a database-level PESSIMISTIC_WRITE lock.
     * Used by redeem to serialise concurrent requests (D-003).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM Member m WHERE m.id = :id")
    Optional<Member> findByIdWithLock(@Param("id") Long id);
}

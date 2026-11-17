package com.swayam.rewards.repository;

import com.swayam.rewards.model.PointsTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface PointsTransactionRepository extends JpaRepository<PointsTransaction, Long> {

    /**
     * Returns all transactions for a member, newest first.
     */
    @Query("SELECT t FROM PointsTransaction t WHERE t.member.id = :memberId ORDER BY t.createdAt DESC")
    List<PointsTransaction> findByMemberIdOrderByCreatedAtDesc(@Param("memberId") Long memberId);
}


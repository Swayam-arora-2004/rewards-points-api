package com.swayam.rewards.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Records every earn or redeem event for a member.
 */
@Entity
@Table(name = "points_transaction")
public class PointsTransaction {

    public enum Type { EARN, REDEEM }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Type type;

    @Column(nullable = false)
    private long points;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected PointsTransaction() {}

    public PointsTransaction(Member member, Type type, long points) {
        this.member = member;
        this.type = type;
        this.points = points;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public Member getMember() { return member; }
    public Type getType() { return type; }
    public long getPoints() { return points; }
    public Instant getCreatedAt() { return createdAt; }
}


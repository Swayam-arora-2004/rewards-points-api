package com.swayam.rewards.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Represents a loyalty programme member.
 * Balance is kept here so a single row-level lock (PESSIMISTIC_WRITE)
 * on this entity prevents concurrent overdrafts (D-003).
 */
@Entity
@Table(name = "member")
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private long balance;

    // Required by JPA
    protected Member() {}

    public Member(String name, String email) {
        this.name = name;
        this.email = email;
        this.balance = 0L;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public long getBalance() { return balance; }

    public void addPoints(long points) {
        this.balance += points;
    }

    /**
     * Deducts points. Caller must check sufficiency and hold a PESSIMISTIC_WRITE
     * lock before calling this (see MemberService.redeem – D-003).
     */
    public void deductPoints(long points) {
        this.balance -= points;
    }
}


package com.swayam.rewards.unit;

import com.swayam.rewards.dto.BalanceResponse;
import com.swayam.rewards.dto.CreateMemberRequest;
import com.swayam.rewards.exception.DuplicateEmailException;
import com.swayam.rewards.exception.InsufficientBalanceException;
import com.swayam.rewards.exception.MemberNotFoundException;
import com.swayam.rewards.model.Member;
import com.swayam.rewards.model.PointsTransaction;
import com.swayam.rewards.repository.MemberRepository;
import com.swayam.rewards.repository.PointsTransactionRepository;
import com.swayam.rewards.service.MemberService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for MemberService.
 * No Spring context is loaded – Mockito stubs the repositories.
 */
@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PointsTransactionRepository transactionRepository;

    @InjectMocks
    private MemberService memberService;

    private Member member;

    @BeforeEach
    void setUp() {
        member = new Member("Alice", "alice@example.com");
    }

    // ── earn ──────────────────────────────────────────────────────────────────

    @Test
    void earn_validPoints_returnsUpdatedBalance() {
        member.addPoints(50); // pre-seed so member has 50
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(transactionRepository.save(any(PointsTransaction.class))).thenAnswer(i -> i.getArgument(0));

        BalanceResponse response = memberService.earn(1L, 100L);

        assertThat(response.balance()).isEqualTo(150L);
    }

    @Test
    void earn_unknownMember_throwsMemberNotFoundException() {
        when(memberRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.earn(99L, 50L))
                .isInstanceOf(MemberNotFoundException.class);
    }

    // ── redeem ────────────────────────────────────────────────────────────────

    @Test
    void redeem_sufficientBalance_returnsUpdatedBalance() {
        member.addPoints(100);
        when(memberRepository.findByIdWithLock(1L)).thenReturn(Optional.of(member));
        when(transactionRepository.save(any(PointsTransaction.class))).thenAnswer(i -> i.getArgument(0));

        BalanceResponse response = memberService.redeem(1L, 30L);

        assertThat(response.balance()).isEqualTo(70L);
    }

    @Test
    void redeem_insufficientBalance_throwsInsufficientBalanceException() {
        member.addPoints(20);
        when(memberRepository.findByIdWithLock(1L)).thenReturn(Optional.of(member));

        assertThatThrownBy(() -> memberService.redeem(1L, 50L))
                .isInstanceOf(InsufficientBalanceException.class)
                .hasMessageContaining("Insufficient balance");
    }

    @Test
    void redeem_unknownMember_throwsMemberNotFoundException() {
        when(memberRepository.findByIdWithLock(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.redeem(99L, 10L))
                .isInstanceOf(MemberNotFoundException.class);
    }

    // ── createMember ──────────────────────────────────────────────────────────

    @Test
    void createMember_duplicateEmail_throwsDuplicateEmailException() {
        when(memberRepository.existsByEmail("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> memberService.createMember(
                new CreateMemberRequest("Alice", "alice@example.com")))
                .isInstanceOf(DuplicateEmailException.class);
    }
}

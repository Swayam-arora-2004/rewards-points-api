package com.swayam.rewards.service;

import com.swayam.rewards.dto.BalanceResponse;
import com.swayam.rewards.dto.CreateMemberRequest;
import com.swayam.rewards.dto.MemberMapper;
import com.swayam.rewards.dto.MemberResponse;
import com.swayam.rewards.dto.TransactionResponse;
import com.swayam.rewards.exception.DuplicateEmailException;
import com.swayam.rewards.exception.InsufficientBalanceException;
import com.swayam.rewards.exception.MemberNotFoundException;
import com.swayam.rewards.model.Member;
import com.swayam.rewards.model.PointsTransaction;
import com.swayam.rewards.repository.MemberRepository;
import com.swayam.rewards.repository.PointsTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * All business rules live here (Coding Standard).
 * Controllers delegate to this service; entities never contain business logic.
 */
@Service
public class MemberService {

    private static final Logger log = LoggerFactory.getLogger(MemberService.class);

    private final MemberRepository memberRepository;
    private final PointsTransactionRepository transactionRepository;

    public MemberService(MemberRepository memberRepository,
                         PointsTransactionRepository transactionRepository) {
        this.memberRepository = memberRepository;
        this.transactionRepository = transactionRepository;
    }

    /**
     * Creates a new member with zero balance.
     * Throws DuplicateEmailException (→ 409) if the email is already registered.
     */
    @Transactional
    public Member createMember(CreateMemberRequest request) {
        if (memberRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException(request.email());
        }
        Member member = new Member(request.name(), request.email());
        Member saved = memberRepository.save(member);
        log.info("Created member id={}", saved.getId());
        return saved;
    }

    /**
     * Adds points to a member's balance and records the EARN transaction.
     * Both the balance update and the transaction insert happen in one database transaction.
     */
    @Transactional
    public BalanceResponse earn(Long memberId, long points) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberNotFoundException(memberId));

        member.addPoints(points);
        transactionRepository.save(new PointsTransaction(member, PointsTransaction.Type.EARN, points));
        log.info("Member id={} earned {} points; new balance={}", memberId, points, member.getBalance());
        return new BalanceResponse(member.getBalance());
    }

    /**
     * Deducts points from a member's balance using a pessimistic lock (D-003).
     *
     * The lock (SELECT … FOR UPDATE) is acquired before reading the balance.
     * This means two concurrent redeem calls for the same member are serialised:
     * the second one waits at the database level until the first transaction commits.
     * This guarantees the balance never goes negative.
     *
     * Throws InsufficientBalanceException (→ 409) when points > balance.
     */
    @Transactional
    public BalanceResponse redeem(Long memberId, long points) {
        // Lock the row before reading the balance – prevents concurrent overdraft (D-003)
        Member member = memberRepository.findByIdWithLock(memberId)
                .orElseThrow(() -> new MemberNotFoundException(memberId));

        if (points > member.getBalance()) {
            throw new InsufficientBalanceException(points, member.getBalance());
        }

        member.deductPoints(points);
        transactionRepository.save(new PointsTransaction(member, PointsTransaction.Type.REDEEM, points));
        log.info("Member id={} redeemed {} points; new balance={}", memberId, points, member.getBalance());
        return new BalanceResponse(member.getBalance());
    }

    /**
     * Returns member details with full transaction history (newest first).
     * Throws MemberNotFoundException (→ 404) for an unknown id.
     */
    @Transactional(readOnly = true)
    public MemberResponse getMember(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberNotFoundException(memberId));

        List<TransactionResponse> history = transactionRepository
                .findByMemberIdOrderByCreatedAtDesc(memberId)
                .stream()
                .map(TransactionResponse::from)
                .toList();

        return MemberMapper.toResponse(member, history);
    }
}


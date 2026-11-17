package com.swayam.rewards.controller;

import com.swayam.rewards.dto.BalanceResponse;
import com.swayam.rewards.dto.CreateMemberRequest;
import com.swayam.rewards.dto.MemberResponse;
import com.swayam.rewards.dto.PointsRequest;
import com.swayam.rewards.model.Member;
import com.swayam.rewards.service.MemberService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes the 4 REST endpoints defined in Project_Context.md.
 * Business logic is entirely in MemberService (Coding Standard).
 */
@RestController
@RequestMapping("/members")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    /** POST /members – create a new member. Returns 201 with the member id. */
    @PostMapping
    public ResponseEntity<Member> createMember(@Valid @RequestBody CreateMemberRequest request) {
        Member created = memberService.createMember(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /** POST /members/{id}/earn – add points; returns 200 with new balance. */
    @PostMapping("/{id}/earn")
    public ResponseEntity<BalanceResponse> earn(
            @PathVariable Long id,
            @Valid @RequestBody PointsRequest request) {
        return ResponseEntity.ok(memberService.earn(id, request.points()));
    }

    /** POST /members/{id}/redeem – spend points; returns 200 or 409 if insufficient. */
    @PostMapping("/{id}/redeem")
    public ResponseEntity<BalanceResponse> redeem(
            @PathVariable Long id,
            @Valid @RequestBody PointsRequest request) {
        return ResponseEntity.ok(memberService.redeem(id, request.points()));
    }

    /** GET /members/{id} – returns balance and transaction history newest-first; 404 if unknown. */
    @GetMapping("/{id}")
    public ResponseEntity<MemberResponse> getMember(@PathVariable Long id) {
        return ResponseEntity.ok(memberService.getMember(id));
    }
}


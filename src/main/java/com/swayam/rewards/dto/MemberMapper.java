package com.swayam.rewards.dto;

import com.swayam.rewards.model.Member;
import java.util.List;

/**
 * Maps a Member entity + its transactions into a MemberResponse DTO.
 */
public final class MemberMapper {

    private MemberMapper() {}

    public static MemberResponse toResponse(Member member, List<TransactionResponse> transactions) {
        return new MemberResponse(
                member.getId(),
                member.getName(),
                member.getEmail(),
                member.getBalance(),
                transactions
        );
    }
}

package com.ktb.cafeboo.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class EmailDuplicationResponse {
    private String email;
    private boolean isDuplicated;
}

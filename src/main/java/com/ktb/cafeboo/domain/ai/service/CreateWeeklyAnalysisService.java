package com.ktb.cafeboo.domain.ai.service;

import com.ktb.cafeboo.global.infra.ai.client.AiServerClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreateWeeklyAnalysisService {
    private final AiServerClient aiServerClient;


}

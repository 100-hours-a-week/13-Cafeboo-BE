package com.ktb.cafeboo.domain.drink.service;

import com.ktb.cafeboo.domain.drink.model.Cafe;
import com.ktb.cafeboo.domain.drink.repository.CafeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CafeService {
    private final CafeRepository cafeRepository;
    public Cafe findCafeById(Long cafeId){
        Cafe target = cafeRepository.findById(cafeId)
            .orElseThrow(() -> new IllegalArgumentException("Drink not found"));

        return target;
    }
}

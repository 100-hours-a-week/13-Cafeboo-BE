package com.ktb.cafeboo.global.censorship.controller;

import com.ktb.cafeboo.global.censorship.filters.TrieCensorshipFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

@Profile({"local", "dev"})
@RestController
@RequestMapping("/internal/censorship")
@RequiredArgsConstructor
public class CensorshipTestController {

    private final TrieCensorshipFilter censorshipFilter;

    @GetMapping("/check")
    public String checkText(@RequestParam String text) {
        boolean contains = censorshipFilter.contains(text);
        return contains
                ? String.format("⚠️ 금칙어가 포함되어 있습니다: \"%s\"", text)
                : String.format("✅ 금칙어가 포함되어 있지 않습니다: \"%s\"", text);
    }
}
package com.ktb.cafeboo.global.censorship;

import com.ktb.cafeboo.global.censorship.filters.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TextCensorshipFilter {

    private final TrieCensorshipFilter trieFilter;
    private final AiCensorshipFilter aiFilter;

    public boolean containsBadWord(String text, CensorshipStrategy strategy) {
        return switch (strategy) {
            case TRIE_ONLY -> trieFilter.contains(text);
            case AI_ONLY -> aiFilter.contains(text);
            case BOTH -> trieFilter.contains(text) || aiFilter.contains(text);
        };
    }
}

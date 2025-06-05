package com.ktb.cafeboo.domain.tag.service;

import com.ktb.cafeboo.domain.coffeechat.model.CoffeeChat;
import com.ktb.cafeboo.domain.tag.model.CoffeeChatTag;
import com.ktb.cafeboo.domain.tag.model.Tag;
import com.ktb.cafeboo.domain.tag.repository.CoffeeChatTagRepository;
import com.ktb.cafeboo.domain.tag.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TagService {

    private final TagRepository tagRepository;
    private final CoffeeChatTagRepository coffeeChatTagRepository;

    public void saveTagsToCoffeeChat(CoffeeChat chat, List<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) {
            return;
        }

        for (String tagName : tagNames) {
            Tag tag = tagRepository.findByName(tagName)
                    .orElseGet(() -> tagRepository.save(Tag.builder().name(tagName).build()));

            CoffeeChatTag coffeeChatTag = new CoffeeChatTag(chat, tag);
            coffeeChatTagRepository.save(coffeeChatTag);
        }
    }
}
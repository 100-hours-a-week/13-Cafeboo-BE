package com.ktb.cafeboo.global.censorship.filters;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ktb.cafeboo.global.infra.s3.S3Downloader;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TrieCensorshipFilter {

    private final S3Downloader s3Downloader;

    @Value("${censorship.trie.filename}")
    private String filename;

    private final TrieNode root = new TrieNode();

    @PostConstruct
    public void init() {
        try {
            List<String> keywords = s3Downloader.downloadKeywordLines(filename);
            keywords.forEach(this::insert);
            log.info("[TrieCensorshipFilter] 금칙어 {}개 로딩 완료", keywords.size());
        } catch (Exception e) {
            log.error("[TrieCensorshipFilter] 금칙어 로딩 실패", e);
        }
    }

    public boolean contains(String text) {
        for (int i = 0; i < text.length(); i++) {
            TrieNode node = root;
            for (int j = i; j < text.length(); j++) {
                char c = text.charAt(j);
                node = node.children.get(c);
                if (node == null) break;
                if (node.isEnd) {
                    log.info("[TrieCensorshipFilter] 금칙어 감지: '{}'", text.substring(i, j + 1));
                    return true;
                }
            }
        }
        return false;
    }

    private void insert(String word) {
        TrieNode node = root;
        for (char c : word.toCharArray()) {
            node = node.children.computeIfAbsent(c, k -> new TrieNode());
        }
        node.isEnd = true;
    }

    private static class TrieNode {
        Map<Character, TrieNode> children = new HashMap<>();
        boolean isEnd = false;
    }
}
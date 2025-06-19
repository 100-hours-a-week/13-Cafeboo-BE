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

    @Value("${censorship.blacklist-filename}")
    private String blacklistFilename;

    @Value("${censorship.whitelist-filename}")
    private String whitelistFilename;

    private final TrieNode blacklistRoot = new TrieNode();
    private final TrieNode whitelistRoot = new TrieNode();

    @PostConstruct
    public void init() {
        try {
            List<String> blackList = s3Downloader.downloadKeywordLines(blacklistFilename);
            List<String> whiteList = s3Downloader.downloadKeywordLines(whitelistFilename);

            blackList.forEach(word -> insert(word, blacklistRoot));
            whiteList.forEach(word -> insert(word, whitelistRoot));

            log.info("[TrieCensorshipFilter] 블랙리스트 {}개, 화이트리스트 {}개 로딩 완료", blackList.size(), whiteList.size());
        } catch (Exception e) {
            log.error("[TrieCensorshipFilter] 금칙어/예외어 로딩 실패", e);
        }
    }

    public boolean contains(String text) {
        for (int i = 0; i < text.length(); i++) {
            TrieNode node = blacklistRoot;
            for (int j = i; j < text.length(); j++) {
                char c = text.charAt(j);
                node = node.children.get(c);
                if (node == null) break;
                if (node.isEnd) {
                    String detected = text.substring(i, j + 1);
                    if (!isWhitelisted(detected, text)) {
                        log.info("[TrieCensorshipFilter] 금칙어 감지: '{}'", detected);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean search(String word, TrieNode root) {
        TrieNode node = root;
        for (char c : word.toCharArray()) {
            node = node.children.get(c);
            if (node == null) return false;
        }
        return node.isEnd;
    }

    private boolean isWhitelisted(String detected, String text) {
        for (int i = 0; i <= text.length() - detected.length(); i++) {
            for (int j = i + detected.length(); j <= text.length(); j++) {
                String candidate = text.substring(i, j);
                if (candidate.contains(detected) && search(candidate, whitelistRoot)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void insert(String word, TrieNode root) {
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
package com.ktb.cafeboo.domain.tag.repository;

import com.ktb.cafeboo.domain.tag.model.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Long> {
    Optional<Tag> findByName(String name);
}
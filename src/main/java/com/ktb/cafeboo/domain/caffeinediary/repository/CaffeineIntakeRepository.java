package com.ktb.cafeboo.domain.caffeinediary.repository;

import com.ktb.cafeboo.domain.caffeinediary.model.CaffeineIntake;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CaffeineIntakeRepository extends JpaRepository<CaffeineIntake, Long> {

}

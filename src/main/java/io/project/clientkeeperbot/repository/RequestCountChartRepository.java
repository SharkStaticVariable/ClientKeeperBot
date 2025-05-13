package io.project.clientkeeperbot.repository;

import io.project.clientkeeperbot.entity.RequestCountChart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RequestCountChartRepository extends JpaRepository<RequestCountChart, Long> {
//    List<RequestCountChart> findByGeneratedByAdminIdOrderByGeneratedAtDesc(Long adminId);
}

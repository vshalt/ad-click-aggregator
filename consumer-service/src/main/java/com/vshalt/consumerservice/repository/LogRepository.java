package com.vshalt.consumerservice.repository;

import com.vshalt.consumerservice.model.LogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LogRepository extends JpaRepository<LogEntity, String> {
}
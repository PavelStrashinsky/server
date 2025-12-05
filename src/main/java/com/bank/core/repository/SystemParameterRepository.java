package com.bank.core.repository;

import com.bank.core.domain.SystemParameter;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemParameterRepository extends JpaRepository<SystemParameter, String> {
}
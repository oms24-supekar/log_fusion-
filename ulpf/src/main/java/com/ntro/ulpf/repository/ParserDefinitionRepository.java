package com.ntro.ulpf.repository;

import com.ntro.ulpf.entity.ParserDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ParserDefinitionRepository
        extends JpaRepository<ParserDefinition, UUID> {

    List<ParserDefinition> findByEnabledTrueOrderByCreatedAtDesc();

    List<ParserDefinition> findAllByOrderByCreatedAtDesc();
}
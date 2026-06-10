package com.gestion.commercial.repository;

import com.gestion.commercial.entity.Local;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LocalRepository extends JpaRepository<Local, Long> {
    List<Local> findByType(String type);
}
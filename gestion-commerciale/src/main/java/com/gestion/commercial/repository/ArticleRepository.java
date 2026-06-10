package com.gestion.commercial.repository;

import com.gestion.commercial.entity.Article;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ArticleRepository extends JpaRepository<Article, Long> {
    Optional<Article> findByReference(String reference);
    List<Article> findByDesignationContainingIgnoreCase(String mot);
}
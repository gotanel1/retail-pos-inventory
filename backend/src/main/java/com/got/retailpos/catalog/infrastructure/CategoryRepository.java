package com.got.retailpos.catalog.infrastructure;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.got.retailpos.catalog.domain.Category;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

	Optional<Category> findByNormalizedName(String normalizedName);

	boolean existsByNormalizedName(String normalizedName);
}

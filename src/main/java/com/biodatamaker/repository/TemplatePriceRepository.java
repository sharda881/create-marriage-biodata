package com.biodatamaker.repository;

import com.biodatamaker.entity.TemplatePrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TemplatePriceRepository extends JpaRepository<TemplatePrice, String> {
}

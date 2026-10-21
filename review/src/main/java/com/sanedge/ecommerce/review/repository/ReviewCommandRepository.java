package com.sanedge.ecommerce.review.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sanedge.ecommerce.review.models.Review;

@Repository
public interface ReviewCommandRepository extends JpaRepository<Review, Long>, ReviewCommandRepositoryCustom {
}

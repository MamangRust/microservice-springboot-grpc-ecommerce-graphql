package com.sanedge.ecommerce.banner.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sanedge.ecommerce.banner.models.Banner;

@Repository
public interface BannerCommandRepository extends JpaRepository<Banner, Long>, BannerCommandRepositoryCustom {

}

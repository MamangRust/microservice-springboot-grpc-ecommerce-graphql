package com.sanedge.ecommerce.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sanedge.ecommerce.user.models.User;

import jakarta.transaction.Transactional;

@Repository
public interface UserCommandRepository extends JpaRepository<User, Long>, UserCommandRepositoryCustom {

    @Modifying
    @Transactional
    @Query("DELETE FROM User u WHERE u.userId = :userId AND u.deletedAt IS NOT NULL")
    int deletePermanent(@Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.deletedAt = NULL WHERE u.deletedAt IS NOT NULL")
    int restoreAllDeleted();

    @Modifying
    @Transactional
    @Query("DELETE FROM User u WHERE u.deletedAt IS NOT NULL")
    int deleteAllDeleted();
}

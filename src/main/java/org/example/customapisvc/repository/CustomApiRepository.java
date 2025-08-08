package org.example.customapisvc.repository;

import org.example.customapisvc.domain.Entity.CustomApi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomApiRepository extends JpaRepository<CustomApi, String> {

    List<CustomApi> findByUserIdAndDeletedFalse(String userId);

    Optional<CustomApi> findByCustomApiIdAndDeletedFalse(String customApiId);

    @Query("SELECT ca FROM CustomApi ca WHERE ca.userId = :userId AND ca.name LIKE %:name% AND ca.deleted = false")
    List<CustomApi> findByUserIdAndNameContainingAndDeletedFalse(@Param("userId") String userId, @Param("name") String name);

    boolean existsByCustomApiIdAndDeletedFalse(String customApiId);
}
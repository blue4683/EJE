package com.skala.miniproject.user.repository;

import com.skala.miniproject.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface WithdrawalUserLockRepository extends JpaRepository<User, Long> {

    @Query(value = "select * from users where id = :id for update", nativeQuery = true)
    Optional<User> findByIdForUpdate(@Param("id") Long id);
}

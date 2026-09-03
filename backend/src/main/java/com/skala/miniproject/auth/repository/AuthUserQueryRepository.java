package com.skala.miniproject.auth.repository;

import com.skala.miniproject.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthUserQueryRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
}

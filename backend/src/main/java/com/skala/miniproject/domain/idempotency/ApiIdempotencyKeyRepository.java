package com.skala.miniproject.domain.idempotency;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiIdempotencyKeyRepository extends JpaRepository<ApiIdempotencyKey, Long> {
}

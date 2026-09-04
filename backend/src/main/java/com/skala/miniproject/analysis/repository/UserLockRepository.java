package com.skala.miniproject.analysis.repository;

import com.skala.miniproject.domain.user.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * UserRepository(A 소유, §9-5 메서드 0개 동결)와 별개로 B 가 §C4 락 순서
 * ("제출·재시도·녹음삭제·회원탈퇴는 users 행을 먼저 FOR UPDATE 로 잠근다")를 구현하기 위해 쓴다.
 * A4(회원탈퇴)·A6(녹음삭제)은 같은 목적의 잠금을 자신의 소유 파일에 별도로 둔다 — 같은 엔티티에
 * 여러 Repository 를 두는 것은 Spring Data 에서 허용된다.
 */
public interface UserLockRepository extends JpaRepository<User, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> lockById(@Param("id") Long id);
}

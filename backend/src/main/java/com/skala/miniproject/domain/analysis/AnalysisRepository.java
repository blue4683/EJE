package com.skala.miniproject.domain.analysis;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Step 0 에서 메서드 0개로 동결한다 (§9-5). 이후 필요한 쿼리는 각자
 * AnalysisQueryRepository(A) / AnalysisWriteRepository(B) 를 새 파일로 만들어 추가한다.
 */
public interface AnalysisRepository extends JpaRepository<Analysis, Long> {
}

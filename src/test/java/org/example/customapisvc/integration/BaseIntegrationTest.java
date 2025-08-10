package org.example.customapisvc.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * 통합 테스트를 위한 기본 추상 클래스
 * 전체 Spring Boot 애플리케이션 컨텍스트를 로드하여 실제 환경과 유사한 테스트 수행
 * 
 * 주요 기능:
 * - 전체 Spring Boot 애플리케이션 컨텍스트 로딩
 * - 실제 HTTP 서버 시작 (RANDOM_PORT)
 * - 모든 레이어 간의 상호작용 테스트
 * - @Transactional을 통한 테스트 데이터 자동 롤백
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
public abstract class BaseIntegrationTest {

    @LocalServerPort
    protected int port;

    protected String getBaseUrl() {
        return "http://localhost:" + port;
    }
}
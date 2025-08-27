package org.example.customapisvc.repository;

import org.example.customapisvc.domain.Entity.CustomApi;
import org.example.customapisvc.testdata.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test") 
@EnableJpaAuditing
@DisplayName("CustomApiRepository 테스트")
class CustomApiRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CustomApiRepository customApiRepository;

    private CustomApi testCustomApi1;
    private CustomApi testCustomApi2;
    private CustomApi deletedCustomApi;

    @BeforeEach
    void setUp() {
        // 테스트 데이터 준비 - TestDataFactory 사용
        testCustomApi1 = TestDataFactory.CustomApiTestData.createCustomApi("api-001", "user-123", "날씨 조회 API", "날씨 정보를 조회하는 API", false);
        testCustomApi1.setCallCount(5L); // 초기 호출 횟수 설정
        testCustomApi2 = TestDataFactory.CustomApiTestData.createCustomApi("api-002", "user-123", "상품 추천 API", "사용자 맞춤 상품을 추천하는 API", false);
        testCustomApi2.setCallCount(10L); // 초기 호출 횟수 설정
        deletedCustomApi = TestDataFactory.CustomApiTestData.createCustomApi("api-003", "user-123", "삭제된 API", "삭제된 API", true);
        
        entityManager.persistAndFlush(testCustomApi1);
        entityManager.persistAndFlush(testCustomApi2);
        entityManager.persistAndFlush(deletedCustomApi);
        entityManager.clear();
    }

    @Test
    @DisplayName("사용자 ID로 삭제되지 않은 커스텀 API 조회")
    void findByUserIdAndDeletedFalse() {
        // when
        List<CustomApi> result = customApiRepository.findByUserIdAndDeletedFalse("user-123");

        // then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(CustomApi::getCustomApiId)
                .containsExactlyInAnyOrder("api-001", "api-002");
        assertThat(result).allMatch(api -> !api.getDeleted());
    }

    @Test
    @DisplayName("존재하지 않는 사용자 ID로 조회시 빈 리스트 반환")
    void findByUserIdAndDeletedFalse_NotExistUser() {
        // when
        List<CustomApi> result = customApiRepository.findByUserIdAndDeletedFalse("nonexistent-user");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("커스텀 API ID로 삭제되지 않은 API 조회")
    void findByCustomApiIdAndDeletedFalse() {
        // when
        Optional<CustomApi> result = customApiRepository.findByCustomApiIdAndDeletedFalse("api-001");

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getCustomApiId()).isEqualTo("api-001");
        assertThat(result.get().getName()).isEqualTo("날씨 조회 API");
        assertThat(result.get().getDeleted()).isFalse();
    }

    @Test
    @DisplayName("삭제된 커스텀 API ID로 조회시 빈 Optional 반환")
    void findByCustomApiIdAndDeletedFalse_DeletedApi() {
        // when
        Optional<CustomApi> result = customApiRepository.findByCustomApiIdAndDeletedFalse("api-003");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("사용자 ID와 이름으로 커스텀 API 검색")
    void findByUserIdAndNameContainingAndDeletedFalse() {
        // when
        List<CustomApi> result = customApiRepository.findByUserIdAndNameContainingAndDeletedFalse("user-123", "조회");

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("날씨 조회 API");
    }

    @Test
    @DisplayName("일치하는 이름이 없을 때 빈 리스트 반환")
    void findByUserIdAndNameContainingAndDeletedFalse_NoMatch() {
        // when
        List<CustomApi> result = customApiRepository.findByUserIdAndNameContainingAndDeletedFalse("user-123", "존재하지않음");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("커스텀 API 존재 확인")
    void existsByCustomApiIdAndDeletedFalse() {
        // when & then
        assertThat(customApiRepository.existsByCustomApiIdAndDeletedFalse("api-001")).isTrue();
        assertThat(customApiRepository.existsByCustomApiIdAndDeletedFalse("api-003")).isFalse(); // 삭제된 API
        assertThat(customApiRepository.existsByCustomApiIdAndDeletedFalse("nonexistent")).isFalse();
    }

    @Test
    @DisplayName("커스텀 API 저장")
    void save() {
        // given
        CustomApi newCustomApi = TestDataFactory.CustomApiTestData.createCustomApi("api-004", "user-456", "테스트 API", "테스트용 API", false);

        // when
        CustomApi saved = customApiRepository.save(newCustomApi);

        // then
        assertThat(saved.getCustomApiId()).isEqualTo("api-004");
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        
        // DB에서 실제 조회 확인
        Optional<CustomApi> found = customApiRepository.findByCustomApiIdAndDeletedFalse("api-004");
        assertThat(found).isPresent();
    }

    @Test
    @DisplayName("호출 횟수 증가 - 정상 케이스")
    void incrementCallCount_Success() {
        // given
        String customApiId = "api-001";
        Long incrementValue = 3L;
        Long expectedCount = 8L; // 5 + 3

        // when
        int updated = customApiRepository.incrementCallCount(customApiId, incrementValue);

        // then
        assertThat(updated).isEqualTo(1);
        
        // 실제 DB에서 값 확인
        Long actualCount = customApiRepository.findCallCountByCustomApiId(customApiId);
        assertThat(actualCount).isEqualTo(expectedCount);
    }

    @Test
    @DisplayName("호출 횟수 증가 - 존재하지 않는 API")
    void incrementCallCount_NotExists() {
        // given
        String nonExistentApiId = "nonexistent-api";
        Long incrementValue = 5L;

        // when
        int updated = customApiRepository.incrementCallCount(nonExistentApiId, incrementValue);

        // then
        assertThat(updated).isEqualTo(0);
    }

    @Test
    @DisplayName("호출 횟수 증가 - 삭제된 API")
    void incrementCallCount_DeletedApi() {
        // given
        String deletedApiId = "api-003";
        Long incrementValue = 2L;

        // when
        int updated = customApiRepository.incrementCallCount(deletedApiId, incrementValue);

        // then
        assertThat(updated).isEqualTo(0);
    }

    @Test
    @DisplayName("모든 호출 횟수 초기화")
    void resetAllCallCounts() {
        // given - testCustomApi1은 5, testCustomApi2는 10의 호출횟수를 가짐

        // when
        int updated = customApiRepository.resetAllCallCounts();

        // then
        assertThat(updated).isEqualTo(2); // 삭제되지 않은 API 2개

        // 실제 값 확인
        Long count1 = customApiRepository.findCallCountByCustomApiId("api-001");
        Long count2 = customApiRepository.findCallCountByCustomApiId("api-002");
        
        assertThat(count1).isEqualTo(0L);
        assertThat(count2).isEqualTo(0L);
    }

    @Test
    @DisplayName("특정 API 호출 횟수 조회 - 정상 케이스")
    void findCallCountByCustomApiId_Success() {
        // when
        Long count = customApiRepository.findCallCountByCustomApiId("api-001");

        // then
        assertThat(count).isEqualTo(5L);
    }

    @Test
    @DisplayName("특정 API 호출 횟수 조회 - 존재하지 않는 API")
    void findCallCountByCustomApiId_NotExists() {
        // when
        Long count = customApiRepository.findCallCountByCustomApiId("nonexistent-api");

        // then
        assertThat(count).isNull();
    }

    @Test
    @DisplayName("특정 API 호출 횟수 조회 - 삭제된 API")
    void findCallCountByCustomApiId_DeletedApi() {
        // when
        Long count = customApiRepository.findCallCountByCustomApiId("api-003");

        // then
        assertThat(count).isNull();
    }
}
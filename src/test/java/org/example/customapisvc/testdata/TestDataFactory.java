package org.example.customapisvc.testdata;

import org.example.customapisvc.domain.Entity.CustomApi;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 테스트용 더미 데이터 생성 팩토리 클래스
 * 각 엔티티별로 테스트에 필요한 데이터를 생성하는 메소드들을 제공
 */
public class TestDataFactory {

    private static final LocalDateTime NOW = LocalDateTime.now();

    /**
     * CustomApi 엔티티 생성
     */
    public static class CustomApiTestData {
        
        public static CustomApi createDefaultCustomApi() {
            return createCustomApi("api-001", "user-123", "날씨 조회 API", "날씨 정보를 조회하는 API", false);
        }
        
        public static CustomApi createCustomApi(String customApiId, String userId, String name, String description) {
            return createCustomApi(customApiId, userId, name, description, false);
        }
        
        public static CustomApi createCustomApi(String customApiId, String userId, String name, String description, boolean deleted) {
            CustomApi customApi = new CustomApi();
            customApi.setCustomApiId(customApiId);
            customApi.setUserId(userId);
            customApi.setName(name);
            customApi.setDescription(description);
            customApi.setDeleted(deleted);
            customApi.setCreatedAt(NOW);
            customApi.setUpdatedAt(NOW);
            return customApi;
        }
        
        public static List<CustomApi> createMultipleCustomApis() {
            return List.of(
                createCustomApi("api-001", "user-123", "날씨 조회 API", "날씨 정보를 조회하는 API"),
                createCustomApi("api-002", "user-123", "상품 추천 API", "사용자 맞춤 상품을 추천하는 API"),
                createCustomApi("api-003", "user-456", "주문 조회 API", "사용자의 주문 내역을 조회하는 API")
            );
        }
        
        public static CustomApi createDeletedCustomApi() {
            return createCustomApi("api-deleted", "user-123", "삭제된 API", "삭제된 API", true);
        }
    }

}
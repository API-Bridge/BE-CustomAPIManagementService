package org.example.customapisvc.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.customapisvc.domain.Entity.CustomApi;
import org.example.customapisvc.dto.response.CustomApiResponseDto;
import org.example.customapisvc.repository.CustomApiRepository;
import org.example.customapisvc.service.impl.CustomApiServiceImple;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomApiService implements CustomApiServiceImple {

    private final CustomApiRepository customApiRepository;

    @Override
    public List<CustomApiResponseDto> getCustomApisByUserId(String userId) {
        log.debug("사용자 이름으로 커스텀API 조회: {}", userId);
        
        List<CustomApi> customApis = customApiRepository.findByUserIdAndDeletedFalse(userId);
        return customApis.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public CustomApiResponseDto getCustomApiById(String customApiId) {
        log.debug("ID로 커스텀API 조회: {}", customApiId);
        
        CustomApi customApi = customApiRepository.findByCustomApiIdAndDeletedFalse(customApiId)
                .orElseThrow(() -> new RuntimeException("커스텀API를 찾을 수 없습니다. customApiId: " + customApiId));
        
        return convertToResponse(customApi);
    }

    @Override
    public List<CustomApiResponseDto> searchCustomApisByName(String userId, String name) {
        log.debug("사용자 이름으로 커스텀API 검색: {} 다음 이름을 포함한 커스텀API 검색: {}", userId, name);
        
        List<CustomApi> customApis = customApiRepository.findByUserIdAndNameContainingAndDeletedFalse(userId, name);
        return customApis.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }


    @Override
    @Transactional
    public void deleteCustomApi(String customApiId, String userId) {
        log.info("Deleting custom API: {} by user: {}", customApiId, userId);
        
        CustomApi customApi = customApiRepository.findByCustomApiIdAndDeletedFalse(customApiId)
                .orElseThrow(() -> new RuntimeException("커스텀API 를 찾을 수 없습니다. customApiId: " + customApiId));
        
        // 사용자 권한 확인
        if (!customApi.getUserId().equals(userId)) {
            throw new RuntimeException("커스텀API 삭제 권한이 없습니다. customApiId: " + customApiId);
        }
        
        // Soft Delete
        customApi.setDeleted(true);
        customApiRepository.save(customApi);
        
        log.info("커스텀API가 성공적으로 삭제되었습니다. customApiId: {}", customApiId);
    }

    //엔티티 -> DTO 변환 메소드
    private CustomApiResponseDto convertToResponse(CustomApi customApi) { // TODO: 외부 API 정보도 함께 반환해야 함
        return new CustomApiResponseDto(
                customApi.getCustomApiId(),
                customApi.getUserId(),
                customApi.getName(),
                customApi.getDescription(),
                customApi.getExternalApiUrlList(),
                customApi.getCreatedAt(),
                customApi.getUpdatedAt()
        );
    }
}
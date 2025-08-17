package org.example.customapisvc.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.customapisvc.domain.Entity.CustomApi;
import org.example.customapisvc.dto.ExternalApiInfoDto;
import org.example.customapisvc.dto.request.ExternalApiRequestDto;
import org.example.customapisvc.dto.request.InitiateCreationRequestDto;
import org.example.customapisvc.dto.response.CustomApiResponseDto;
import org.example.customapisvc.dto.response.ExternalApiResponseDto;
import org.example.customapisvc.repository.CustomApiRepository;
import org.example.customapisvc.service.AiCustomApiGenerationService;
import org.example.customapisvc.service.ExternalApiService;
import org.example.customapisvc.service.cache.DisabledApiCache;
import org.example.customapisvc.util.GenerateTextFromTextInput;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiCustomApiGenerationServiceImpl implements AiCustomApiGenerationService {

    private final GenerateTextFromTextInput geminiService;
    private final CustomApiRepository customApiRepository;
    private final ExternalApiService externalApiService;
    private final DisabledApiCache disabledApiCache;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public CustomApiResponseDto generateCustomApiWithAi(InitiateCreationRequestDto request) {
        log.info("AI 커스텀 API 생성 시작 - userId: {}, customApiId: {}, domains: {}, keywords: {}, userQuery: {}, plan: {}",
                request.getUserId(), request.getCustomApiId(), request.getDomains(), request.getKeywords(), request.getUserQuery(), request.getPlan());

        try {
            // 1단계: 외부API서비스에서 도메인/키워드에 해당하는 외부 API 리스트 조회
            ExternalApiRequestDto externalApiRequest = new ExternalApiRequestDto(request.getDomains(), request.getKeywords());
            ExternalApiResponseDto externalApiResponse = externalApiService.getExternalApiList(externalApiRequest);
            List<ExternalApiInfoDto> availableExternalApis = externalApiResponse.getExternalApiList();
            log.info("외부API서비스로부터 {} 개의 외부 API 수신", availableExternalApis.size());

            // 1.5단계: 캐시에서 비활성화된 외부 API 필터링
            Set<String> disabledApiIds = disabledApiCache.getDisabledApis();
            List<ExternalApiInfoDto> filteredApiList = availableExternalApis;
            if (disabledApiIds != null && !disabledApiIds.isEmpty()) {
                log.info("비활성화된 API {}개를 필터링합니다: {}", disabledApiIds.size(), disabledApiIds);
                filteredApiList = availableExternalApis.stream()
                        .filter(api -> !disabledApiIds.contains(api.getApiId()))
                        .collect(Collectors.toList());
                log.info("필터링 후 {}개의 외부 API가 남았습니다.", filteredApiList.size());
            }

            if (filteredApiList.isEmpty()) {
                log.warn("사용 가능한 외부 API가 없어 AI 커스텀 API 생성을 중단합니다. customApiId: {}", request.getCustomApiId());
                throw new RuntimeException("사용 가능한 외부 API가 없습니다.");
            }

            // 2단계: Gemini AI에게 커스텀 API 생성 요청 (필터링된 외부 API 리스트와 함께)
            String aiPrompt = createPromptForCustomApiGeneration(request, filteredApiList);
            String aiResponse = geminiService.generateText(aiPrompt);

            log.debug("Gemini AI 응답: {}", aiResponse);

            // 3단계: AI 응답을 기반으로 커스텀 API 메타데이터 생성
            CustomApiMetadata metadata = parseAiResponseToMetadata(aiResponse, filteredApiList);

            // 4단계: 데이터베이스에 커스텀 API 저장
            CustomApi customApi = new CustomApi();
            customApi.setCustomApiId(request.getCustomApiId());
            customApi.setUserId(request.getUserId());
            customApi.setName(metadata.getName());
            customApi.setDescription(metadata.getDescription());
            customApi.setExternalApiUrlList(metadata.getSelectedExternalApis());
            customApi.setCreatedAt(LocalDateTime.now());
            customApi.setUpdatedAt(LocalDateTime.now());
            customApi.setDeleted(false);

            CustomApi savedApi = customApiRepository.save(customApi);

            log.info("AI 커스텀 API 생성 완료 - customApiId: {}", savedApi.getCustomApiId());

            // 5단계: 응답 DTO 생성
            return new CustomApiResponseDto(
                    savedApi.getCustomApiId(),
                    savedApi.getUserId(),
                    savedApi.getName(),
                    savedApi.getDescription(),
                    savedApi.getExternalApiUrlList(),
                    savedApi.getCreatedAt(),
                    savedApi.getUpdatedAt()
            );

        } catch (Exception e) {
            log.error("AI 커스텀 API 생성 실패 - customApiId: {}", request.getCustomApiId(), e);
            throw new RuntimeException("AI를 활용한 커스텀 API 생성에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * Gemini AI에게 보낼 프롬프트 생성
     */
    private String createPromptForCustomApiGeneration(InitiateCreationRequestDto request, List<ExternalApiInfoDto> externalApiList) throws JsonProcessingException {
        // 외부 API 리스트를 JSON 문자열로 변환
        String externalApiListJson = objectMapper.writeValueAsString(externalApiList);

        // 플랜에 따른 최대 API 개수 결정
        int maxApiCount = "PRO".equalsIgnoreCase(request.getPlan()) ? 5 : 3;

        return String.format("""
                당신은 외부 API를 분석하고 사용자의 요구사항에 맞는 최적의 API를 선별하는 전문가입니다.

                **사용자 요구사항:**
                "%s"

                **사용자 플랜:** %s (최대 %d개 API 선별 가능)

                **사용 가능한 외부 API 리스트:**
                %s

                **분석 요청:**
                1. 사용자의 요구사항을 분석하여 가장 적절한 외부 API를 최대 %d개까지 선별해주세요
                2. 선별 기준: 사용자 요구사항과의 연관성, API 기능의 적합성, 파라미터의 유용성
                3. 선별된 API들의 데이터 의존성을 분석하여 호출 순서를 결정해주세요
                4. 병렬 호출이 가능한 API들은 같은 호출 순서 번호를 부여하고 parallelGroup으로 표시해주세요

                **응답 형식:**
                반드시 다음 JSON 형식으로만 답변해주세요:

                {
                  "name": "커스텀 API 이름 (사용자 요구사항을 반영한 명확하고 구체적인 이름)",
                  "description": "커스텀 API 설명 (선별된 API들이 어떻게 조합되어 사용자 요구사항을 만족하는지 설명)",
                  "selectedApis": [
                    {
                      "apiId": "선별된 API의 ID",
                      "apiName": "선별된 API의 이름",
                      "callOrder": 1,
                      "parallelGroup": "A",
                      "reason": "이 API를 선택한 구체적인 이유",
                      "dependency": "NONE",
                      "parameters": [
                        {
                          "paramName": "파라미터 이름",
                          "paramType": "INPUT/OUTPUT",
                          "description": "파라미터 설명",
                          "necessary": true
                        }
                      ]
                    }
                  ]
                }

                **호출 순서 결정 기준:**
                - callOrder는 데이터 의존성을 기준으로 결정합니다 (1부터 시작)
                - 다른 API의 출력값을 입력으로 사용하는 API는 더 높은 번호를 가집니다
                - 데이터 의존성이 없어 병렬 호출이 가능한 API들은 같은 callOrder를 가집니다
                - parallelGroup은 같은 callOrder 내에서 병렬 호출되는 API들을 구분합니다 (A, B, C...)
                - dependency 필드에는 의존하는 API의 출력 파라미터를 명시하고, 의존성이 없으면 'NONE'을 입력합니다

                **주의사항:**
                - selectedApis 배열은 최대 %d개까지만 포함하세요
                - 모든 API에 대해 parallelGroup과 dependency 필드는 반드시 포함해야 합니다
                - 데이터 의존성을 정확히 분석하여 올바른 호출 순서를 결정하세요
                - 병렬 처리 가능한 API들을 명확히 표시하세요
                - JSON 형식 외의 다른 텍스트는 절대 포함하지 마세요
                - 모든 텍스트는 한국어로 작성하세요
                """,
                request.getUserQuery(),
                request.getPlan(),
                maxApiCount,
                externalApiListJson,
                maxApiCount,
                maxApiCount
        );
    }

    /**
     * AI 응답을 파싱하여 커스텀 API 메타데이터로 변환
     */
    private CustomApiMetadata parseAiResponseToMetadata(String aiResponse, List<ExternalApiInfoDto> allExternalApis) throws JsonProcessingException {
        // AI 응답에서 마크다운 코드 블록 제거
        String cleanedResponse = aiResponse.trim();
        if (cleanedResponse.startsWith("```json")) {
            cleanedResponse = cleanedResponse.substring(7);
        } else if (cleanedResponse.startsWith("```")) {
            cleanedResponse = cleanedResponse.substring(3);
        }
        if (cleanedResponse.endsWith("```")) {
            cleanedResponse = cleanedResponse.substring(0, cleanedResponse.length() - 3);
        }
        cleanedResponse = cleanedResponse.trim();
        
        log.debug("정제된 AI 응답: {}", cleanedResponse);
        
        // JSON 형태의 AI 응답 파싱
        JsonNode jsonNode = objectMapper.readTree(cleanedResponse);

        String name = jsonNode.get("name").asText();
        String description = jsonNode.get("description").asText();

        List<ExternalApiInfoDto> selectedExternalApis = new ArrayList<>();

        // selectedApis 정보 처리
        if (jsonNode.has("selectedApis")) {
            JsonNode selectedApisNode = jsonNode.get("selectedApis");
            log.info("AI가 선별한 API 개수: {}", selectedApisNode.size());
            
            for (int i = 0; i < selectedApisNode.size(); i++) {
                JsonNode apiNode = selectedApisNode.get(i);
                String apiId = apiNode.get("apiId").asText();
                String reason = apiNode.get("reason").asText();
                int callOrder = apiNode.has("callOrder") ? apiNode.get("callOrder").asInt() : i + 1;
                
                log.info("선별된 API {}: {} (호출순서: {}, 이유: {})", i + 1, apiId, callOrder, reason);

                // 전체 외부 API 리스트에서 해당 API 찾아서 추가
                allExternalApis.stream()
                    .filter(api -> api.getApiId().equals(apiId))
                    .findFirst()
                    .ifPresent(selectedExternalApis::add);
            }
        }

        return new CustomApiMetadata(name, description, selectedExternalApis);
    }

    /**
     * 커스텀 API 메타데이터를 담는 내부 클래스
     */
    @Getter
    private static class CustomApiMetadata {
        private final String name;
        private final String description;
        private final List<ExternalApiInfoDto> selectedExternalApis;

        public CustomApiMetadata(String name, String description, List<ExternalApiInfoDto> selectedExternalApis) {
            this.name = name;
            this.description = description;
            this.selectedExternalApis = selectedExternalApis;
        }
    }
}

package org.example.customapisvc.util;

import com.google.genai.Client;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class GenerateTextFromTextInput {
  
  @Value("${gemini.api.key:}")
  private String apiKey;
  
  private Client geminiClient;
  
  public String generateText(String prompt) {
    try {
      // API 키가 설정되지 않은 경우 예외 처리
      if (apiKey == null || apiKey.trim().isEmpty()) {
        throw new IllegalStateException("Gemini API 키가 설정되지 않았습니다.");
      }
      
      // Gemini Client 초기화 (lazy initialization)
      if (geminiClient == null) {
        log.info("Google GenAI Client 초기화 중...");
        geminiClient = Client.builder()
            .apiKey(apiKey)
            .build();
      }
      
      log.debug("Gemini API 요청 시작: prompt length = {}", prompt.length());
      
      // GenerateContent 요청 생성 및 실행
      var response = geminiClient.models.generateContent(
          "gemini-1.5-flash-latest",
          prompt,
          null
      );
      
      String result = response.text();
      log.debug("Gemini API 응답 완료: response length = {}", result != null ? result.length() : 0);
      
      if (result == null || result.trim().isEmpty()) {
        throw new RuntimeException("Gemini API의 응답이 비어있습니다.");
      }
      
      return result;
      
    } catch (Exception e) {
      log.error("Gemini API 요청 실패", e);
      throw new RuntimeException("Gemini API 요청에 실패했습니다: " + e.getMessage(), e);
    }
  }
  
  // 테스트용 main 메서드
  public static void main(String[] args) {
    String apiKey = System.getenv("GEMINI_API_KEY");
    if (apiKey == null || apiKey.trim().isEmpty()) {
      System.out.println("설정된 GEMINI_API_KEY 환경변수가 없습니다.");
      return;
    }
    
    GenerateTextFromTextInput generator = new GenerateTextFromTextInput();
    // 임시로 API 키 설정 (테스트용)
    try {
      java.lang.reflect.Field field = GenerateTextFromTextInput.class.getDeclaredField("apiKey");
      field.setAccessible(true);
      field.set(generator, apiKey);
      
      String result = generator.generateText("Explain how AI works in a few words");
      System.out.println("Gemini Response: " + result);
    } catch (Exception e) {
      System.err.println("Error: " + e.getMessage());
      e.printStackTrace();
    }
  }
}
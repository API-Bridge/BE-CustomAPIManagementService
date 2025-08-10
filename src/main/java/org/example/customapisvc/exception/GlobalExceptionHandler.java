package org.example.customapisvc.exception;

import org.example.customapisvc.dto.common.BaseResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 글로벌 예외 처리 핸들러
 * 컨트롤러에서 발생하는 예외를 적절한 HTTP 상태 코드와 응답으로 변환
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<BaseResponse<Void>> handleRuntimeException(RuntimeException e) {
        String message = e.getMessage();
        
        // 메시지 기반으로 적절한 HTTP 상태 코드 결정
        if (message != null) {
            if (message.contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(BaseResponse.error(message));
            }
            
            if (message.contains("Unauthorized")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(BaseResponse.error(message));
            }
        }
        
        // 기타 RuntimeException은 500 Internal Server Error
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(BaseResponse.error("서버 내부 오류가 발생했습니다."));
    }
}
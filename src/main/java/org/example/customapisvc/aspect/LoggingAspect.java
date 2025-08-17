package org.example.customapisvc.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.example.customapisvc.util.StructuredLogger;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * AOP를 사용한 로깅 및 성능 모니터링 Aspect
 * 컨트롤러와 서비스 레이어의 메소드 실행에 대한 로깅 및 성능 데이터를 수집
 * 
 * 주요 기능:
 * - 메소드 실행 시간 측정 및 로깅
 * - 예외 발생 시 오류 로깅
 * - 메소드 입출력 매개변수 및 반환값 로깅 (디버그 모드)
 * - 컨트롤러와 서비스 레이어를 구분한 로깅
 */
@Slf4j
@Aspect
@Component
public class LoggingAspect {

    private final StructuredLogger structuredLogger;

    public LoggingAspect(StructuredLogger structuredLogger) {
        this.structuredLogger = structuredLogger;
    }

    /** 컨트롤러 레이어의 모든 메소드를 대상으로 하는 포인트컷 */
    @Pointcut("execution(* org.example.customapisvc.controller..*(..))")
    public void controllerPointcut() {}

    /** 서비스 레이어의 모든 메소드를 대상으로 하는 포인트컷 */
    @Pointcut("execution(* org.example.customapisvc.service..*(..))")
    public void servicePointcut() {}

    /**
     * 컨트롤러 메소드 실행 시간을 측정하고 로깅하는 Around 어드바이스
     */
    @Around("controllerPointcut()")
    public Object logExecutionTimeController(ProceedingJoinPoint joinPoint) throws Throwable {
        return logExecutionTime(joinPoint, "CONTROLLER");
    }

    /**
     * 서비스 메소드 실행 시간을 측정하고 로깅하는 Around 어드바이스
     */
    @Around("servicePointcut()")
    public Object logExecutionTimeService(ProceedingJoinPoint joinPoint) throws Throwable {
        return logExecutionTime(joinPoint, "SERVICE");
    }

    /**
     * 메소드 실행 시간을 측정하고 로깅하는 내부 메소드
     * 실행 성공 시 실행 시간을 로깅하고, 예외 발생 시 오류를 로깅
     * 
     * @param joinPoint AOP 조인포인트 객체
     * @param layer 레이어 구분자 (CONTROLLER 또는 SERVICE)
     * @return 메소드 실행 결과
     * @throws Throwable 메소드 실행 중 발생할 수 있는 예외
     */
    private Object logExecutionTime(ProceedingJoinPoint joinPoint, String layer) throws Throwable {
        StopWatch stopWatch = new StopWatch();
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();
        String fullMethodName = className + "." + methodName;

        Map<String, Object> additionalFields = new HashMap<>();
        additionalFields.put("layer", layer);
        additionalFields.put("class_name", className);
        additionalFields.put("method_name", methodName);
        additionalFields.put("full_method_name", fullMethodName);

        try {
            stopWatch.start();
            Object result = joinPoint.proceed();
            stopWatch.stop();

            long executionTime = stopWatch.getTotalTimeMillis();
            additionalFields.put("execution_time_ms", executionTime);
            additionalFields.put("status", "SUCCESS");

            // 구조화된 성능 로깅
            structuredLogger.logPerformanceEvent(fullMethodName, executionTime, "SUCCESS", additionalFields);

            log.info("[{}] {}.{} executed in {} ms",
                layer, className, methodName, executionTime);

            return result;
        } catch (Exception e) {
            stopWatch.stop();
            long executionTime = stopWatch.getTotalTimeMillis();
            
            additionalFields.put("execution_time_ms", executionTime);
            additionalFields.put("status", "FAILED");
            additionalFields.put("exception_type", e.getClass().getSimpleName());
            additionalFields.put("error_message", e.getMessage());

            // 구조화된 에러 로깅
            structuredLogger.logError("METHOD_EXECUTION_FAILED", 
                "Method execution failed in " + layer + " layer", e, additionalFields);

            log.error("[{}] {}.{} failed after {} ms with exception: {}",
                layer, className, methodName, executionTime, e.getMessage());
            throw e;
        }
    }

    /**
     * 컨트롤러 메소드 실행 전 매개변수를 로깅 (DEBUG 레벨)
     */
    @Before("controllerPointcut()")
    public void logBefore(JoinPoint joinPoint) {
        log.debug("Entering method: {} with arguments: {}",
            joinPoint.getSignature().toShortString(),
            Arrays.toString(joinPoint.getArgs()));
    }

    /**
     * 컨트롤러 메소드 정상 실행 후 반환값을 로깅 (DEBUG 레벨)
     */
    @AfterReturning(pointcut = "controllerPointcut()", returning = "result")
    public void logAfterReturning(JoinPoint joinPoint, Object result) {
        log.debug("Method: {} returned: {}",
            joinPoint.getSignature().toShortString(), result);
    }

    /**
     * 컨트롤러 및 서비스 메소드에서 예외 발생 시 로깅 (ERROR 레벨)
     */
    @AfterThrowing(pointcut = "controllerPointcut() || servicePointcut()", throwing = "ex")
    public void logAfterThrowing(JoinPoint joinPoint, Throwable ex) {
        String methodSignature = joinPoint.getSignature().toShortString();
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();
        
        Map<String, Object> additionalFields = new HashMap<>();
        additionalFields.put("method_signature", methodSignature);
        additionalFields.put("class_name", className);
        additionalFields.put("method_name", methodName);
        additionalFields.put("exception_type", ex.getClass().getSimpleName());
        
        // 레이어 구분
        String layer = className.contains(".controller.") ? "CONTROLLER" : "SERVICE";
        additionalFields.put("layer", layer);
        
        structuredLogger.logError("UNHANDLED_EXCEPTION", 
            "Unhandled exception in " + layer + " layer", ex, additionalFields);
        
        log.error("Method: {} threw exception: {}",
            methodSignature, ex.getMessage());
    }
}
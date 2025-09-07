-- =====================================================
-- Association Table 모델로 마이그레이션 스크립트
-- =====================================================
-- 
-- 이 스크립트는 기존 자기참조 방식의 API 공유 시스템을
-- Association Table 방식으로 안전하게 마이그레이션합니다.
--
-- 실행 순서:
-- 1. 데이터 백업
-- 2. api_share 테이블 생성 (이미 schema.sql에 포함)
-- 3. 기존 LINK 타입 데이터를 api_share로 이관
-- 4. 데이터 검증
-- 5. 기존 컬럼 제거 (별도 스크립트)
-- =====================================================

-- 1. 마이그레이션 전 데이터 백업 (선택사항)
-- CREATE TABLE custom_api_backup AS SELECT * FROM custom_api WHERE api_type = 'LINK';

-- 2. 기존 LINK 타입 데이터를 api_share 테이블로 이관
-- LINK 타입의 custom_api 레코드를 기반으로 api_share 관계 생성
INSERT INTO api_share (origin_api_id, user_id, shared_at)
SELECT 
    ca.origin_api_id as origin_api_id,
    ca.user_id as user_id,
    ca.created_at as shared_at
FROM custom_api ca 
WHERE ca.api_type = 'LINK' 
  AND ca.origin_api_id IS NOT NULL
  AND ca.deleted = false
  AND NOT EXISTS (
      -- 중복 방지: 이미 api_share에 존재하는 관계는 제외
      SELECT 1 FROM api_share ash 
      WHERE ash.origin_api_id = ca.origin_api_id 
        AND ash.user_id = ca.user_id
  );

-- 3. 마이그레이션 결과 검증 쿼리들
-- 이관 전 LINK 타입 레코드 수
-- SELECT COUNT(*) as link_count_before FROM custom_api WHERE api_type = 'LINK' AND deleted = false;

-- 이관 후 api_share 레코드 수  
-- SELECT COUNT(*) as share_count_after FROM api_share;

-- 데이터 무결성 검증: 모든 api_share의 origin_api_id가 실제 존재하는지 확인
-- SELECT COUNT(*) as orphan_shares 
-- FROM api_share ash 
-- LEFT JOIN custom_api ca ON ash.origin_api_id = ca.custom_api_id 
-- WHERE ca.custom_api_id IS NULL OR ca.deleted = true;

-- 4. LINK 타입 레코드 정리 (소프트 삭제)
-- 이관이 완료된 후 기존 LINK 타입 레코드들을 소프트 삭제 처리
UPDATE custom_api 
SET deleted = true, 
    updated_at = CURRENT_TIMESTAMP(6)
WHERE api_type = 'LINK' 
  AND deleted = false
  AND EXISTS (
      -- api_share에 대응되는 관계가 존재하는 경우만
      SELECT 1 FROM api_share ash 
      WHERE ash.origin_api_id = custom_api.origin_api_id 
        AND ash.user_id = custom_api.user_id
  );

-- 5. 마이그레이션 완료 후 최종 검증
-- 활성 상태인 LINK 타입 레코드가 없어야 함
-- SELECT COUNT(*) as remaining_link_records 
-- FROM custom_api 
-- WHERE api_type = 'LINK' AND deleted = false;

-- api_share 테이블의 모든 관계가 유효한 custom_api를 참조하는지 확인
-- SELECT COUNT(*) as valid_shares
-- FROM api_share ash 
-- INNER JOIN custom_api ca ON ash.origin_api_id = ca.custom_api_id 
-- WHERE ca.deleted = false;

-- =====================================================
-- 롤백 스크립트 (문제 발생 시 사용)
-- =====================================================
/*
-- LINK 타입 레코드 복구
UPDATE custom_api 
SET deleted = false, 
    updated_at = CURRENT_TIMESTAMP(6)
WHERE api_type = 'LINK' 
  AND EXISTS (
      SELECT 1 FROM api_share ash 
      WHERE ash.origin_api_id = custom_api.origin_api_id 
        AND ash.user_id = custom_api.user_id
  );

-- api_share 테이블 데이터 삭제 (테이블은 유지)
-- DELETE FROM api_share;

-- 백업에서 데이터 복구 (백업을 생성한 경우)
-- INSERT INTO custom_api SELECT * FROM custom_api_backup;
-- DROP TABLE custom_api_backup;
*/

-- =====================================================
-- 마이그레이션 완료 후 정리 작업 (별도 실행 권장)
-- =====================================================
/*
-- custom_api 테이블에서 더 이상 필요없는 컬럼들 제거
-- 주의: 애플리케이션 코드 업데이트 완료 후에만 실행할 것!

ALTER TABLE custom_api 
DROP COLUMN api_type,
DROP COLUMN origin_api_id,
DROP INDEX idx_api_type,
DROP INDEX idx_origin_api_id,
DROP FOREIGN KEY fk_custom_api_origin;
*/
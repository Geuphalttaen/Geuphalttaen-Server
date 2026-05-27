-- PR #34: SyncStatus에 IN_PROGRESS 추가, status 컬럼을 MySQL ENUM으로 변환
-- 실행 시점: feature/async-sync develop 머지 전
ALTER TABLE sync_logs
    MODIFY COLUMN status ENUM('IN_PROGRESS', 'SUCCESS', 'PARTIAL', 'FAILED') NOT NULL;

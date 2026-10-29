-- 시설 코드(facilities) 기본 데이터. 서버가 켜질 때마다 실행됩니다.
-- INSERT IGNORE 라서 이미 있는 코드는 건너뜁니다(중복 오류 없음). 항목은 필요한 만큼 추가해도 됩니다.
INSERT IGNORE INTO facilities (facility_code, facility_name, sort_order, use_yn, created_at, updated_at) VALUES
('PARKING', '주차', 1, true, NOW(), NOW()),
('WIFI', '와이파이', 2, true, NOW(), NOW()),
('BREAKFAST', '조식', 3, true, NOW(), NOW());

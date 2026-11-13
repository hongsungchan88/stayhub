-- 시설 코드(facilities) 기본 데이터. 서버가 켜질 때마다 실행됩니다.
-- INSERT IGNORE 라서 이미 있는 코드는 건너뜁니다(중복 오류 없음). 항목은 필요한 만큼 추가해도 됩니다.
INSERT IGNORE INTO facilities (facility_code, facility_name, sort_order, use_yn, created_at, updated_at) VALUES
('PARKING', '주차', 1, true, NOW(), NOW()),
('WIFI', '와이파이', 2, true, NOW(), NOW()),
('BREAKFAST', '조식', 3, true, NOW(), NOW());

-- 채널(channels) 기본 데이터. 채널 등록 API가 생기기 전에도 결제를 등록할 수 있게 넣어 둡니다(PAY-003).
-- channel_name에는 중복 제한이 없어서, channel_id를 직접 정해 INSERT IGNORE로 한 번만 들어가게 합니다.
-- 1번: 직접 예약(등록 채널, 수수료 0%). 2번: 기타 채널(미등록 채널, 기본 수수료율 10% 적용).
INSERT IGNORE INTO channels (channel_id, channel_name, commission_rate, is_registered, default_commission_rate, created_at, updated_at) VALUES
(1, '직접 예약', 0.00, true, 10.00, NOW(), NOW()),
(2, '기타 채널', 0.00, false, 10.00, NOW(), NOW());

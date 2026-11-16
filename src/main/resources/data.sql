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

-- 통합 인박스(COM-001) 샘플. 채널 연동이 없는 1차에서 화면을 바로 볼 수 있게 넣어 둡니다.
-- 고정 ID + INSERT IGNORE 라서 한 번만 들어갑니다. 더 넣으려면 POST /api/message-threads/inbound(시연용 모의 수신).
-- 1번(직접 예약)·3번(기타 채널)은 마지막 메시지가 게스트 것이라 "수동 응대 필요"가 붙고, 2번은 답장 완료.
INSERT IGNORE INTO message_threads (thread_id, channel_id, reservation_id, guest_name, sync_status, last_message_at, is_read, created_at, updated_at) VALUES
(1, 1, NULL, '김민수', 'SUCCESS', NOW() - INTERVAL 10 MINUTE, false, NOW() - INTERVAL 2 HOUR, NOW()),
(2, 2, NULL, 'Emily Clark', 'SUCCESS', NOW() - INTERVAL 1 HOUR, true, NOW() - INTERVAL 1 DAY, NOW()),
(3, 2, NULL, '박지영', 'SUCCESS', NOW() - INTERVAL 3 HOUR, false, NOW() - INTERVAL 3 HOUR, NOW());

INSERT IGNORE INTO messages (message_id, thread_id, direction, content, send_status, retry_count, sent_by, created_at, updated_at) VALUES
(1, 1, 'IN', '안녕하세요. 체크인 전에 짐을 맡길 수 있을까요?', NULL, 0, NULL, NOW() - INTERVAL 2 HOUR, NOW()),
(2, 1, 'OUT', '네, 오후 1시부터 프런트에서 보관해 드립니다.', 'SUCCESS', 0, NULL, NOW() - INTERVAL 90 MINUTE, NOW()),
(3, 1, 'IN', '감사합니다. 주차도 가능한지 궁금합니다.', NULL, 0, NULL, NOW() - INTERVAL 10 MINUTE, NOW()),
(4, 2, 'IN', 'Is late check-out available on Sunday?', NULL, 0, NULL, NOW() - INTERVAL 1 DAY, NOW()),
(5, 2, 'OUT', 'Yes, until 1 PM at no extra charge.', 'SUCCESS', 0, NULL, NOW() - INTERVAL 1 HOUR, NOW()),
(6, 3, 'IN', '객실에 아기 침대를 추가할 수 있나요?', NULL, 0, NULL, NOW() - INTERVAL 3 HOUR, NOW());

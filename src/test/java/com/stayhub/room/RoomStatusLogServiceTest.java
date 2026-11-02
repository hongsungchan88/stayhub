package com.stayhub.room;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.stayhub.common.BusinessException;
import com.stayhub.common.PageResponse;
import com.stayhub.room.dto.RoomStatusLogResponse;
import com.stayhub.room.entity.RoomStatus;
import com.stayhub.room.entity.RoomStatusLog;
import com.stayhub.room.repository.RoomStatusLogRepository;
import com.stayhub.room.service.RoomStatusLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * 객실 상태 변경 이력 서비스 테스트.
 *
 * <p>실제 DB(application-local.yml 의 MySQL)에 붙어서 조회가 제대로 되는지 확인합니다.
 * {@code @Transactional} 이 붙어 있어서 테스트가 끝나면 넣었던 데이터가 전부 되돌려집니다(롤백).
 * 이력은 상태 변경이 자동으로 남기므로, 등록은 리포지토리로 바로 넣고 조회만 확인합니다.
 */
@SpringBootTest
@Transactional
class RoomStatusLogServiceTest {

	@Autowired
	private RoomStatusLogService roomStatusLogService;

	@Autowired
	private RoomStatusLogRepository roomStatusLogRepository;

	@BeforeEach
	void setUp() {
		// 개발 DB 에 이미 들어 있는 데이터 때문에 결과가 흔들리지 않도록 테스트 안에서만 비웁니다.
		// @Transactional 이라 테스트가 끝나면 지운 것까지 원래대로 돌아옵니다.
		// 경고: @Transactional을 지우면 개발 DB의 이 테이블이 실제로 비워집니다. 지우지 마세요.
		roomStatusLogRepository.deleteAll();
	}

	@Test
	@DisplayName("남긴 이력이 목록에 나온다")
	void savedLogAppearsInList() {
		RoomStatusLog saved = saveLog("체크아웃");

		PageResponse<RoomStatusLogResponse> all = roomStatusLogService.getList(0);
		assertThat(all.content()).extracting(RoomStatusLogResponse::id).containsExactly(saved.getId());

		// 충돌 여부는 비워 보내면 false 로 시작한다
		assertThat(all.content().get(0).isConflict()).isFalse();
	}

	@Test
	@DisplayName("상세 조회하면 남긴 값이 나온다")
	void getDetailReturnsSavedValues() {
		RoomStatusLog saved = saveLog("체크아웃");

		RoomStatusLogResponse found = roomStatusLogService.getDetail(saved.getId());
		assertThat(found.roomId()).isEqualTo(1L);
		assertThat(found.previousStatus()).isEqualTo(RoomStatus.OCCUPIED);
		assertThat(found.newStatus()).isEqualTo(RoomStatus.CLEANING_NEEDED);
		assertThat(found.changeReason()).isEqualTo("체크아웃");
		assertThat(found.changedBy()).isEqualTo(1L);
	}

	@Test
	@DisplayName("없는 항목을 상세 조회하면 BusinessException 이 난다")
	void getMissingThrows() {
		assertThatThrownBy(() -> roomStatusLogService.getDetail(99999L))
				.isInstanceOf(BusinessException.class)
				.hasMessage("객실 상태 변경 이력을 찾을 수 없습니다.");
	}

	/** 테스트용 이력을 저장합니다. 객실·상태·담당자는 테스트마다 중요하지 않아 고정값으로 둡니다. */
	private RoomStatusLog saveLog(String changeReason) {
		return roomStatusLogRepository.save(RoomStatusLog.create(
				1L, RoomStatus.OCCUPIED, RoomStatus.CLEANING_NEEDED, changeReason, 1L, null));
	}
}

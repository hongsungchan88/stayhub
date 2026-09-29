package com.stayhub.content;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.stayhub.common.BusinessException;
import com.stayhub.common.EntityStatus;
import com.stayhub.common.PageResponse;
import com.stayhub.content.dto.GuidebookCreateRequest;
import com.stayhub.content.dto.GuidebookResponse;
import com.stayhub.content.dto.GuidebookUpdateRequest;
import com.stayhub.content.entity.GuidebookCategory;
import com.stayhub.content.repository.GuidebookRepository;
import com.stayhub.content.service.GuidebookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * 가이드북 서비스 테스트.
 *
 * <p><b>이 파일이 하는 일</b>
 * <br>실제 DB(application-local.yml 의 MySQL)에 붙어서 등록·조회·수정·삭제가 제대로 되는지 확인합니다.
 * {@code @Transactional} 이 붙어 있어서 테스트가 끝나면 넣었던 데이터가 전부 되돌려집니다(롤백).
 * 개발 DB 가 지저분해질 걱정은 하지 않아도 됩니다.
 *
 * <p>실행하려면 MySQL 이 켜져 있고 {@code application-local.yml} 이 있어야 합니다.
 * IntelliJ 에서는 클래스 이름 왼쪽의 초록 화살표, 터미널에서는 {@code ./gradlew test}.
 *
 * <p><b>따라 만들 때 바꿀 곳</b>
 * <ul>
 *   <li>패키지·클래스명 — 내 도메인과 {@code Xxx + ServiceTest}</li>
 *   <li>주입받는 서비스·리포지토리 타입</li>
 *   <li>{@code createRequest} 도우미 — 내 요청 DTO 에 맞는 값으로</li>
 *   <li>중복 제목 테스트 — 가이드북만의 규칙입니다. 내 테이블에 없으면 테스트째 지웁니다</li>
 * </ul>
 * 나머지 네 개(등록·수정·삭제·삭제 후 조회)는 모든 테이블에 똑같이 필요합니다. 지우지 마세요.
 */
@SpringBootTest
@Transactional
class GuidebookServiceTest { // ★ 바꿀 곳: 클래스명

	@Autowired
	private GuidebookService guidebookService; // ★ 바꿀 곳: 서비스 타입

	@Autowired
	private GuidebookRepository guidebookRepository; // ★ 바꿀 곳: 리포지토리 타입

	@BeforeEach
	void setUp() {
		// 개발 DB 에 이미 들어 있는 데이터 때문에 결과가 흔들리지 않도록 테스트 안에서만 비웁니다.
		// @Transactional 이라 테스트가 끝나면 지운 것까지 원래대로 돌아옵니다.
		// 경고: @Transactional을 지우면 개발 DB의 이 테이블이 실제로 비워집니다. 지우지 마세요.
		guidebookRepository.deleteAll(); // ★ 바꿀 곳: 리포지토리
	}

	@Test
	@DisplayName("등록하면 목록에 나온다")
	void createThenAppearsInList() {
		GuidebookResponse created = guidebookService.create(createRequest(GuidebookCategory.WIFI, "와이파이 비밀번호"));

		PageResponse<GuidebookResponse> all = guidebookService.getList(null, 0);
		assertThat(all.content()).extracting(GuidebookResponse::id).containsExactly(created.id());

		// 카테고리 필터도 확인 — 같은 카테고리로는 나오고, 다른 카테고리로는 안 나온다
		assertThat(guidebookService.getList(GuidebookCategory.WIFI, 0).totalElements()).isEqualTo(1);
		assertThat(guidebookService.getList(GuidebookCategory.PARKING, 0).totalElements()).isZero();
	}

	@Test
	@DisplayName("수정하면 바뀐 값이 조회된다")
	void updateIsApplied() {
		GuidebookResponse created = guidebookService.create(createRequest(GuidebookCategory.WIFI, "와이파이 비밀번호"));

		guidebookService.update(created.id(), new GuidebookUpdateRequest( // ★ 바꿀 곳: 수정 요청 DTO
				GuidebookCategory.PARKING, "주차 안내", "지하 2층에 주차하세요.", 5));

		GuidebookResponse found = guidebookService.getDetail(created.id());
		assertThat(found.category()).isEqualTo(GuidebookCategory.PARKING); // ★ 바꿀 곳: 확인할 필드
		assertThat(found.title()).isEqualTo("주차 안내");
		assertThat(found.content()).isEqualTo("지하 2층에 주차하세요.");
		assertThat(found.sortOrder()).isEqualTo(5);
	}

	@Test
	@DisplayName("삭제하면 목록에서 빠지고, 행은 DELETED 로 남는다")
	void deleteRemovesFromList() {
		GuidebookResponse created = guidebookService.create(createRequest(GuidebookCategory.WIFI, "와이파이 비밀번호"));

		guidebookService.delete(created.id());

		assertThat(guidebookService.getList(null, 0).totalElements()).isZero();

		// 소프트 삭제라서 DB 에는 행이 그대로 있고 status 만 바뀌었다
		assertThat(guidebookRepository.findById(created.id()))
				.get()
				.extracting(guidebook -> guidebook.getStatus())
				.isEqualTo(EntityStatus.DELETED);
	}

	@Test
	@DisplayName("삭제된 항목을 상세 조회하면 BusinessException 이 난다")
	void getDeletedThrows() {
		GuidebookResponse created = guidebookService.create(createRequest(GuidebookCategory.WIFI, "와이파이 비밀번호"));
		guidebookService.delete(created.id());

		assertThatThrownBy(() -> guidebookService.getDetail(created.id()))
				.isInstanceOf(BusinessException.class)
				.hasMessage("가이드북을 찾을 수 없습니다."); // ★ 바꿀 곳: 예외 메시지
	}

	@Test
	@DisplayName("같은 카테고리에 같은 제목이 있으면 감지하지만, 저장은 막지 않는다")
	void detectsSameTitle() { // ★ 바꿀 곳: 가이드북만의 규칙. 내 테이블에 없으면 이 테스트째 삭제
		guidebookService.create(createRequest(GuidebookCategory.WIFI, "와이파이 비밀번호"));

		assertThat(guidebookService.hasSameTitle(GuidebookCategory.WIFI, "와이파이 비밀번호")).isTrue();
		assertThat(guidebookService.hasSameTitle(GuidebookCategory.PARKING, "와이파이 비밀번호")).isFalse();

		// 경고만 하고 저장은 허용한다
		guidebookService.create(createRequest(GuidebookCategory.WIFI, "와이파이 비밀번호"));
		assertThat(guidebookService.getList(GuidebookCategory.WIFI, 0).totalElements()).isEqualTo(2);
	}

	/** 테스트용 등록 요청. 내용·정렬 순서는 테스트마다 중요하지 않아 고정값으로 둡니다. */
	private GuidebookCreateRequest createRequest(GuidebookCategory category, String title) { // ★ 바꿀 곳: 요청 DTO
		return new GuidebookCreateRequest(category, title, "테스트 내용", null);
	}
}

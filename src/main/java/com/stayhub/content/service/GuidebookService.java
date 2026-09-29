package com.stayhub.content.service;

import com.stayhub.common.BusinessException;
import com.stayhub.common.EntityStatus;
import com.stayhub.common.PageResponse;
import com.stayhub.common.PropertyScope;
import com.stayhub.content.dto.GuidebookCreateRequest;
import com.stayhub.content.dto.GuidebookResponse;
import com.stayhub.content.dto.GuidebookUpdateRequest;
import com.stayhub.content.entity.Guidebook;
import com.stayhub.content.entity.GuidebookCategory;
import com.stayhub.content.repository.GuidebookRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 가이드북 업무 로직.
 *
 * <p><b>이 파일이 하는 일</b>
 * <br>컨트롤러에게 요청 DTO 를 받아 엔티티를 만들거나 고치고, 결과를 응답 DTO 로 바꿔 돌려줍니다.
 * 엔티티는 이 클래스 밖으로 나가지 않습니다.
 *
 * <p><b>트랜잭션 규칙</b>
 * <ul>
 *   <li>클래스에 {@code @Transactional(readOnly = true)} — 조회 메서드는 이걸로 충분합니다</li>
 *   <li>쓰기 메서드(등록·수정·삭제)에만 {@code @Transactional} 을 따로 붙입니다</li>
 * </ul>
 * 이렇게 해두면 조회 메서드에서 실수로 값을 바꿔도 DB 에 반영되지 않고, 쓰기 메서드가 한눈에 보입니다.
 *
 * <p><b>따라 만들 때 바꿀 곳</b>
 * <ul>
 *   <li>클래스명·리포지토리 타입·DTO 타입 — Guidebook 을 내 테이블 이름으로</li>
 *   <li>{@code getList} 의 카테고리 필터 — 내 테이블에 필터가 없으면 if 문째 지웁니다</li>
 *   <li>{@code create} / {@code update} 에서 엔티티로 넘기는 값 — 내 컬럼에 맞게</li>
 *   <li>예외 메시지 — "가이드북" 을 내 기능 이름으로</li>
 *   <li>{@code hasSameTitle} — 가이드북만의 규칙입니다. 내 테이블에 없으면 메서드째 지웁니다</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class GuidebookService { // ★ 바꿀 곳: 클래스명

	/** 목록 정렬: sort_order 오름차순 → 같으면 guidebook_id 오름차순(등록 순). */
	private static final Sort LIST_SORT = Sort.by(
			Sort.Order.asc("sortOrder"), // ★ 바꿀 곳: 정렬 기준 필드 (엔티티 필드명. 컬럼명 아님)
			Sort.Order.asc("id")
	);

	private final GuidebookRepository guidebookRepository; // ★ 바꿀 곳: 리포지토리 타입·이름

	public GuidebookService(GuidebookRepository guidebookRepository) { // ★ 바꿀 곳: 클래스명·리포지토리
		this.guidebookRepository = guidebookRepository;
	}

	/**
	 * 목록. ACTIVE 만, 한 페이지 20건.
	 *
	 * @param category 비어 있으면 전체
	 * @param page     0부터 시작
	 */
	public PageResponse<GuidebookResponse> getList(GuidebookCategory category, int page) { // ★ 바꿀 곳: 응답 타입·필터 파라미터
		Pageable pageable = PageRequest.of(page, PageResponse.DEFAULT_PAGE_SIZE, LIST_SORT);

		Page<Guidebook> guidebooks; // ★ 바꿀 곳: 엔티티 타입
		if (category == null) { // ★ 바꿀 곳: 필터가 없는 테이블은 이 if 문째 지우고 아래 한 줄만 남깁니다
			guidebooks = guidebookRepository.findByPropertyIdAndStatus(
					PropertyScope.DEFAULT_PROPERTY_ID, EntityStatus.ACTIVE, pageable);
		} else {
			guidebooks = guidebookRepository.findByPropertyIdAndCategoryAndStatus(
					PropertyScope.DEFAULT_PROPERTY_ID, category, EntityStatus.ACTIVE, pageable);
		}

		return PageResponse.from(guidebooks.map(GuidebookResponse::from)); // ★ 바꿀 곳: 응답 DTO
	}

	/** 상세. 없거나 삭제된 항목이면 BusinessException. */
	public GuidebookResponse getDetail(Long id) { // ★ 바꿀 곳: 응답 타입
		return GuidebookResponse.from(findActive(id)); // ★ 바꿀 곳: 응답 DTO
	}

	/**
	 * 같은 카테고리에 같은 제목이 이미 있는지. 있어도 저장은 막지 않고 경고만 합니다.
	 * (가이드북만의 규칙 — 내 테이블에 없으면 이 메서드를 지우세요)
	 */
	public boolean hasSameTitle(GuidebookCategory category, String title) {
		return guidebookRepository.existsByPropertyIdAndCategoryAndTitleAndStatus(
				PropertyScope.DEFAULT_PROPERTY_ID, category, title, EntityStatus.ACTIVE);
	}

	/** 등록. property_id 는 요청에서 받지 않고 여기서 상수로 넣습니다. */
	@Transactional
	public GuidebookResponse create(GuidebookCreateRequest request) { // ★ 바꿀 곳: 요청·응답 타입
		Guidebook guidebook = Guidebook.create( // ★ 바꿀 곳: 엔티티 타입
				PropertyScope.DEFAULT_PROPERTY_ID,
				request.category(), // ★ 바꿀 곳: 요청 필드
				request.title(), // ★ 바꿀 곳: 요청 필드
				request.content(), // ★ 바꿀 곳: 요청 필드
				request.sortOrder() // ★ 바꿀 곳: 요청 필드
		);
		return GuidebookResponse.from(guidebookRepository.save(guidebook)); // ★ 바꿀 곳: 응답 DTO
	}

	/** 수정. 없거나 삭제된 항목이면 BusinessException. */
	@Transactional
	public GuidebookResponse update(Long id, GuidebookUpdateRequest request) { // ★ 바꿀 곳: 요청·응답 타입
		Guidebook guidebook = findActive(id); // ★ 바꿀 곳: 엔티티 타입
		guidebook.update(
				request.category(), // ★ 바꿀 곳: 요청 필드
				request.title(), // ★ 바꿀 곳: 요청 필드
				request.content(), // ★ 바꿀 곳: 요청 필드
				request.sortOrder() // ★ 바꿀 곳: 요청 필드
		);
		// save() 를 부르지 않아도 트랜잭션이 끝날 때 바뀐 값이 자동으로 DB 에 반영됩니다. (변경 감지)
		// 다만 수정일시(updatedAt)는 반영되는 순간에 채워지므로, 응답에 새 값을 담으려고 먼저 반영합니다.
		guidebookRepository.flush();
		return GuidebookResponse.from(guidebook); // ★ 바꿀 곳: 응답 DTO
	}

	/** 소프트 삭제. 행은 남기고 status 만 DELETED 로 바꿉니다. */
	@Transactional
	public void delete(Long id) {
		findActive(id).delete();
	}

	/** ACTIVE 인 항목 하나를 찾습니다. 없거나 삭제됐으면 예외. */
	private Guidebook findActive(Long id) { // ★ 바꿀 곳: 엔티티 타입
		return guidebookRepository
				.findByIdAndPropertyIdAndStatus(id, PropertyScope.DEFAULT_PROPERTY_ID, EntityStatus.ACTIVE)
				.orElseThrow(() -> new BusinessException("가이드북을 찾을 수 없습니다.")); // ★ 바꿀 곳: 메시지
	}
}

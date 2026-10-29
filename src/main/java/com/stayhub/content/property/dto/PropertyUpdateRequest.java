package com.stayhub.content.property.dto;

import com.stayhub.content.property.entity.PropertyStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 숙소 수정 요청 (PUT /api/properties/{id} 의 본문).
 *
 * <p>PUT 은 "통째로 바꾸기" 라서 모든 값을 다시 보냅니다.
 * 등록과 달리 게시 상태(status)를 여기서 바꿉니다. (DRAFT ↔ PUBLISHED)
 *
 * <p>{@code facilityCodes} — 체크된 시설 코드 전체 목록. 예) {@code ["PARKING","WIFI"]}.
 * 모두 해제하려면 빈 목록 {@code []} 을 보냅니다. 바뀐 것만 DB 에 반영합니다(PropertyFacilityService.sync).
 */
public record PropertyUpdateRequest(

		@NotBlank(message = "숙소명을 입력해 주세요.")
		@Size(max = 50, message = "숙소명은 50자 이내로 입력해 주세요.")
		String propertyName,

		@NotBlank(message = "소개 문구를 입력해 주세요.")
		@Size(max = 2000, message = "소개 문구는 2,000자 이내로 입력해 주세요.")
		String description,

		@NotNull(message = "게시 상태를 선택해 주세요.")
		PropertyStatus status,

		// 빠뜨리면 시설이 전부 해제된 것으로 오해할 수 있어서 필수로 받습니다. 해제는 [] 로.
		@NotNull(message = "시설 목록을 보내 주세요. 없으면 빈 목록 [] 입니다.")
		List<String> facilityCodes
) {
}

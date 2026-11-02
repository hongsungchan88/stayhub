package com.stayhub.payment.service;

import com.stayhub.common.BusinessException;
import com.stayhub.common.PageResponse;
import com.stayhub.payment.dto.AdditionalChargeCreateRequest;
import com.stayhub.payment.dto.AdditionalChargeResponse;
import com.stayhub.payment.dto.AdditionalChargeUpdateRequest;
import com.stayhub.payment.entity.AdditionalCharge;
import com.stayhub.payment.entity.Payment;
import com.stayhub.payment.repository.AdditionalChargeRepository;
import com.stayhub.payment.repository.PaymentRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 추가 요금 업무 로직.
 *
 * <p>컨트롤러에게 요청 DTO 를 받아 엔티티를 만들거나 고치고, 결과를 응답 DTO 로 바꿔 돌려줍니다.
 * 엔티티는 이 클래스 밖으로 나가지 않습니다.
 *
 * <p><b>트랜잭션 규칙</b> — 클래스에 {@code @Transactional(readOnly = true)}, 쓰기 메서드(등록·수정)에만 {@code @Transactional}.
 */
@Service
@Transactional(readOnly = true)
public class AdditionalChargeService {

	/** 목록 정렬: charge_id 오름차순(등록 순). */
	private static final Sort LIST_SORT = Sort.by(
			Sort.Order.asc("id")
	);

	private final AdditionalChargeRepository additionalChargeRepository;
	private final PaymentRepository paymentRepository;

	public AdditionalChargeService(AdditionalChargeRepository additionalChargeRepository, PaymentRepository paymentRepository) {
		this.additionalChargeRepository = additionalChargeRepository;
		this.paymentRepository = paymentRepository;
	}

	/**
	 * 목록. 한 페이지 20건.
	 *
	 * @param page 0부터 시작
	 */
	public PageResponse<AdditionalChargeResponse> getList(int page) {
		Pageable pageable = PageRequest.of(page, PageResponse.DEFAULT_PAGE_SIZE, LIST_SORT);
		return PageResponse.from(additionalChargeRepository.findAll(pageable).map(AdditionalChargeResponse::from));
	}

	/** 상세. 없는 항목이면 BusinessException. */
	public AdditionalChargeResponse getDetail(Long id) {
		return AdditionalChargeResponse.from(findOrThrow(id));
	}

	/** 등록. 합산 정보가 맞지 않으면 BusinessException. */
	@Transactional
	public AdditionalChargeResponse create(AdditionalChargeCreateRequest request) {
		validateMerge(request.reservationId(), request.isMerged(), request.paymentId());

		AdditionalCharge additionalCharge = AdditionalCharge.create(
				request.reservationId(),
				request.chargeType(),
				request.description(),
				request.amount(),
				request.isMerged(),
				request.paymentId(),
				request.isSettled()
		);
		return AdditionalChargeResponse.from(additionalChargeRepository.save(additionalCharge));
	}

	/** 수정. 없는 항목이거나 합산 정보가 맞지 않으면 BusinessException. */
	@Transactional
	public AdditionalChargeResponse update(Long id, AdditionalChargeUpdateRequest request) {
		AdditionalCharge additionalCharge = findOrThrow(id);
		validateMerge(request.reservationId(), request.isMerged(), request.paymentId());

		additionalCharge.update(
				request.reservationId(),
				request.chargeType(),
				request.description(),
				request.amount(),
				request.isMerged(),
				request.paymentId(),
				request.isSettled()
		);
		// save() 를 부르지 않아도 트랜잭션이 끝날 때 바뀐 값이 자동으로 DB 에 반영됩니다. (변경 감지)
		// 다만 수정일시(updatedAt)는 반영되는 순간에 채워지므로, 응답에 새 값을 담으려고 먼저 반영합니다.
		additionalChargeRepository.flush();
		return AdditionalChargeResponse.from(additionalCharge);
	}

	/**
	 * 합산 정보를 확인합니다 (PAY-002). 합산하면 같은 예약의 결제를 지정해야 하고, 합산하지 않으면 결제를 지정할 수 없습니다.
	 * 이 확인이 없으면 다른 예약의 결제 합계에 금액이 더해질 수 있습니다.
	 */
	private void validateMerge(Long reservationId, Boolean isMerged, Long paymentId) {
		if (!Boolean.TRUE.equals(isMerged)) {
			if (paymentId != null) {
				throw new BusinessException("합산하지 않는 추가 요금에는 결제를 지정할 수 없습니다.");
			}
			return;
		}
		if (paymentId == null) {
			throw new BusinessException("합산할 결제를 선택해 주세요.");
		}
		Payment payment = paymentRepository.findById(paymentId)
				.orElseThrow(() -> new BusinessException("합산할 결제 내역을 찾을 수 없습니다."));
		if (!payment.getReservationId().equals(reservationId)) {
			throw new BusinessException("같은 예약의 결제에만 합산할 수 있습니다.");
		}
	}

	/** 항목 하나를 찾습니다. 없으면 예외. */
	private AdditionalCharge findOrThrow(Long id) {
		return additionalChargeRepository.findById(id)
				.orElseThrow(() -> new BusinessException("추가 요금을 찾을 수 없습니다."));
	}
}

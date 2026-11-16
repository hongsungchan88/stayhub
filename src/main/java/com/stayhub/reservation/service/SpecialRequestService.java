package com.stayhub.reservation.service;

import com.stayhub.account.repository.UserRepository;
import com.stayhub.common.BusinessException;
import com.stayhub.common.EntityStatus;
import com.stayhub.common.PageResponse;
import com.stayhub.reservation.dto.SpecialRequestCreateRequest;
import com.stayhub.reservation.dto.SpecialRequestResponse;
import com.stayhub.reservation.dto.SpecialRequestUpdateRequest;
import com.stayhub.reservation.entity.SpecialRequest;
import com.stayhub.reservation.entity.SpecialRequestStatus;
import com.stayhub.reservation.repository.ReservationRepository;
import com.stayhub.reservation.repository.SpecialRequestRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 특별 요청사항 등록·조회·처리 정보 변경. */
@Service
@Transactional(readOnly = true)
public class SpecialRequestService {

    private final SpecialRequestRepository specialRequestRepository;
    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;

    public SpecialRequestService(
            SpecialRequestRepository specialRequestRepository,
            ReservationRepository reservationRepository,
            UserRepository userRepository
    ) {
        this.specialRequestRepository = specialRequestRepository;
        this.reservationRepository = reservationRepository;
        this.userRepository = userRepository;
    }

    /** 새 요청을 접수 상태로 등록합니다. */
    @Transactional
    public SpecialRequestResponse create(
            SpecialRequestCreateRequest request
    ) {
        validateReservation(request.reservationId());

        if (request.requestContent() == null
                || request.requestContent().isBlank()) {
            throw new BusinessException(
                    "requestContent: 요청 내용을 입력해 주세요.");
        }

        SpecialRequest specialRequest = SpecialRequest.create(
                request.reservationId(),
                request.requestContent().trim()
        );

        return SpecialRequestResponse.from(
                specialRequestRepository.saveAndFlush(specialRequest));
    }

    /**
     * 최신 요청부터 한 페이지에 20건씩 조회합니다.
     * reservationId가 있으면 해당 예약의 요청만 조회합니다.
     */
    public PageResponse<SpecialRequestResponse> getList(
            Long reservationId,
            int page
    ) {
        if (page < 0) {
            throw new BusinessException(
                    "페이지 번호는 0 이상이어야 합니다.");
        }

        PageRequest pageable = PageRequest.of(
                page,
                PageResponse.DEFAULT_PAGE_SIZE,
                Sort.by(Sort.Direction.DESC, "id")
        );

        Page<SpecialRequest> requests;

        if (reservationId == null) {
            requests = specialRequestRepository.findAll(pageable);
        } else {
            validateReservation(reservationId);
            requests = specialRequestRepository.findByReservationId(
                    reservationId, pageable);
        }

        return PageResponse.from(
                requests.map(SpecialRequestResponse::from));
    }

    /** 요청 한 건의 상세 정보를 조회합니다. */
    public SpecialRequestResponse getDetail(Long id) {
        return SpecialRequestResponse.from(findRequest(id));
    }

    /** 상태·거절 사유·담당자를 함께 저장합니다. */
    @Transactional
    public SpecialRequestResponse update(
            Long id,
            SpecialRequestUpdateRequest request
    ) {
        SpecialRequest specialRequest = findRequest(id);

        if (request.status() == null) {
            throw new BusinessException(
                    "status: 처리 상태를 선택해 주세요.");
        }

        String rejectReason = request.rejectReason() == null
                ? null
                : request.rejectReason().trim();

        if (rejectReason != null && rejectReason.length() > 255) {
            throw new BusinessException(
                    "rejectReason: 거절 사유는 255자 이내로 입력해 주세요.");
        }

        if (request.status() == SpecialRequestStatus.REJECTED) {
            if (rejectReason == null || rejectReason.isBlank()) {
                throw new BusinessException(
                        "rejectReason: 거절 사유를 입력해 주세요.");
            }
        } else {
            // 거절 상태가 아니면 이전 거절 사유를 남기지 않습니다.
            rejectReason = null;
        }

        Long staffId = request.assignedStaffId();

        if (staffId != null) {
            if (staffId <= 0) {
                throw new BusinessException(
                        "assignedStaffId: 담당자 값이 올바르지 않습니다.");
            }

            userRepository.findById(staffId)
                    .filter(user -> user.getStatus() == EntityStatus.ACTIVE)
                    .orElseThrow(() -> new BusinessException(
                            "assignedStaffId: 사용 중인 담당자 계정을 찾을 수 없습니다."));
        }

        specialRequest.changeProcessing(
                request.status(),
                rejectReason,
                staffId
        );

        specialRequestRepository.flush();

        return SpecialRequestResponse.from(specialRequest);
    }

    private SpecialRequest findRequest(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(
                    "요청사항 번호가 올바르지 않습니다.");
        }

        return specialRequestRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        "특별 요청사항을 찾을 수 없습니다."));
    }

    private void validateReservation(Long reservationId) {
        if (reservationId == null || reservationId <= 0) {
            throw new BusinessException(
                    "reservationId: 예약을 선택해 주세요.");
        }

        if (!reservationRepository.existsById(reservationId)) {
            throw new BusinessException(
                    "reservationId: 예약을 찾을 수 없습니다.");
        }
    }
}
package com.stayhub.channel.service;

import com.stayhub.channel.entity.ChannelSyncLog;
import com.stayhub.channel.entity.ChannelSyncStatus;
import com.stayhub.channel.repository.ChannelSyncLogRepository;
import com.stayhub.common.BusinessException;
import com.stayhub.reservation.exception.ReservationOverlapException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.stayhub.channel.dto.ChannelSyncLogResponse;
import com.stayhub.common.PageResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/** 모의 채널 예약 가져오기 결과를 기록합니다. */
@Service
@RequiredArgsConstructor
public class ChannelSyncLogService {

    private final ChannelSyncLogRepository logRepository;

    /** 동기화 로그를 최신순으로 한 페이지에 20건씩 조회합니다. */
    @Transactional(readOnly = true)
    public PageResponse<ChannelSyncLogResponse> getList(int page) {
        if (page < 0) {
            throw new BusinessException("페이지 번호는 0 이상이어야 합니다.");
        }
        Pageable pageable = PageRequest.of(page, PageResponse.DEFAULT_PAGE_SIZE);
        return PageResponse.from(logRepository.findAllByOrderBySyncedAtDescIdDesc(pageable)
                .map(ChannelSyncLogResponse::from));
    }

    /** 성공 로그를 예약과 같은 트랜잭션에 저장합니다. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void recordSuccess(String externalId) {
        logRepository.save(ChannelSyncLog.forMockImport(
                externalId, ChannelSyncStatus.SUCCESS, null));
    }

    /** 예약 저장이 실패해도 별도 트랜잭션에 실패 로그를 남깁니다. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(String externalId, RuntimeException failure) {
        ChannelSyncStatus status = failure instanceof ReservationOverlapException
                ? ChannelSyncStatus.CONFLICT : ChannelSyncStatus.FAIL;

        String reason;
        if (failure instanceof BusinessException) {
            reason = failure.getMessage();
        } else if (failure instanceof OptimisticLockingFailureException) {
            reason = "동시에 객실이 변경되어 예약을 저장하지 못했습니다.";
        } else {
            reason = "예약을 가져오는 중 오류가 발생했습니다.";
        }

        logRepository.save(ChannelSyncLog.forMockImport(externalId, status, reason));
    }
}
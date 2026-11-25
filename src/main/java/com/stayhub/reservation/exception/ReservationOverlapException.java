package com.stayhub.reservation.exception;

import com.stayhub.common.BusinessException;

/** 같은 객실의 숙박 날짜가 겹쳐 예약할 수 없을 때 발생합니다. */
public class ReservationOverlapException extends BusinessException {

    /** 화면에 표시할 겹침 사유를 받습니다. */
    public ReservationOverlapException(String message) {
        super(message);
    }
}
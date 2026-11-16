package com.stayhub.reservation.repository;

import com.stayhub.reservation.entity.SpecialRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpecialRequestRepository
        extends JpaRepository<SpecialRequest, Long> {

    Page<SpecialRequest> findByReservationId(
            Long reservationId,
            Pageable pageable
    );
}
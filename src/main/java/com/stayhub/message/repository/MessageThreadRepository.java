package com.stayhub.message.repository;

import com.stayhub.message.entity.MessageThread;
import com.stayhub.reservation.entity.ReservationStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageThreadRepository extends JpaRepository<MessageThread, Long> {

    Page<MessageThread> findByChannelId(Long channelId, Pageable pageable);

    /** 같은 채널·게스트의 가장 최근 스레드(모의 수신이 이어 붙일 곳). */
    Optional<MessageThread> findFirstByChannelIdAndGuestNameOrderByIdDesc(Long channelId, String guestName);

    /**
     * 게스트 이름이 같고 상태가 statuses 인 예약 ID. 예약 도메인 파일을 고치지 않으려고 여기서 조회합니다.
     * 서비스가 정확히 1건일 때만 스레드에 연결합니다.
     */
    @Query("select r.id from Reservation r where r.guestName = :guestName and r.status in :statuses")
    List<Long> findReservationIds(
            @Param("guestName") String guestName,
            @Param("statuses") Collection<ReservationStatus> statuses
    );
}

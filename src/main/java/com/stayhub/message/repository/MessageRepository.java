package com.stayhub.message.repository;

import com.stayhub.message.entity.Message;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByThreadIdOrderByCreatedAtAscIdAsc(Long threadId);

    /** 스레드마다 마지막 메시지 한 건(목록의 미리보기·답장 필요 표시용). */
    @Query("""
            select m from Message m
            where m.id in (
                select max(m2.id) from Message m2
                where m2.threadId in :threadIds
                group by m2.threadId
            )
            """)
    List<Message> findLastMessages(@Param("threadIds") Collection<Long> threadIds);
}

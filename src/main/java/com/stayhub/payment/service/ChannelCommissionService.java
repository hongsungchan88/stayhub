package com.stayhub.payment.service;

import com.stayhub.channel.dto.ChannelResponse;
import com.stayhub.channel.entity.Channel;
import com.stayhub.channel.repository.ChannelRepository;
import com.stayhub.common.BusinessException;
import com.stayhub.payment.dto.ChannelCommissionUpdateRequest;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 채널 수수료율 수정 (PAY-003, 채널 수수료 설정 화면 SCR-PAY-03).
 *
 * <p>채널마다 수수료율({@code commission_rate})과 미등록 채널일 때 쓰는 기본 수수료율({@code default_commission_rate})을 바꿉니다.
 * 바꾼 비율은 그 뒤에 등록·수정하는 결제부터 적용됩니다({@link PaymentService} 가 저장할 때 계산). 이미 저장된 결제는 그대로입니다.
 *
 * <p>{@link Channel} 은 채널 담당(팀원 B) 폴더의 엔티티라 변경 메서드를 넣지 않고, 이 서비스에서 JPQL 수정 쿼리로 두 칼럼만 바꿉니다.
 * 채널 등록·이름·등록 여부는 여기서 바꾸지 않습니다.
 */
@Service
@Transactional(readOnly = true)
public class ChannelCommissionService {

	private final ChannelRepository channelRepository;
	private final EntityManager entityManager;

	public ChannelCommissionService(ChannelRepository channelRepository, EntityManager entityManager) {
		this.channelRepository = channelRepository;
		this.entityManager = entityManager;
	}

	/** 수수료율과 기본 수수료율을 바꿉니다. 채널이 없으면 BusinessException. */
	@Transactional
	public ChannelResponse update(Long channelId, ChannelCommissionUpdateRequest request) {
		Channel channel = channelRepository.findById(channelId)
				.orElseThrow(() -> new BusinessException("예약 채널을 찾을 수 없습니다."));

		entityManager.createQuery("""
						update Channel c
						set c.commissionRate = :commissionRate,
						    c.defaultCommissionRate = :defaultCommissionRate,
						    c.updatedAt = :now
						where c.id = :id
						""")
				.setParameter("commissionRate", request.commissionRate())
				.setParameter("defaultCommissionRate", request.defaultCommissionRate())
				.setParameter("now", LocalDateTime.now())
				.setParameter("id", channelId)
				.executeUpdate();

		entityManager.refresh(channel); // 수정 쿼리는 이미 읽어 둔 엔티티를 바꾸지 않으므로 DB 값으로 다시 읽음
		return ChannelResponse.from(channel);
	}
}

package com.stayhub.payment;

import com.stayhub.channel.entity.Channel;
import com.stayhub.channel.repository.ChannelRepository;
import java.math.BigDecimal;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 결제 테스트용 채널을 저장합니다.
 *
 * <p>{@link Channel} 은 채널 담당 도메인의 엔티티이고 만드는 메서드가 아직 없어서, 그 파일을 고치지 않고 테스트에서만 값을 넣습니다.
 */
final class ChannelFixture {

	private ChannelFixture() {
	}

	/** 수수료율이 {@code commissionRate} 인 채널. 기본 수수료율은 Channel 의 기본값(10)입니다. */
	static Channel save(ChannelRepository channelRepository, String commissionRate, boolean registered) {
		Channel channel = BeanUtils.instantiateClass(Channel.class);
		ReflectionTestUtils.setField(channel, "channelName", "테스트 채널");
		ReflectionTestUtils.setField(channel, "commissionRate", new BigDecimal(commissionRate));
		ReflectionTestUtils.setField(channel, "registered", registered);
		return channelRepository.save(channel);
	}
}

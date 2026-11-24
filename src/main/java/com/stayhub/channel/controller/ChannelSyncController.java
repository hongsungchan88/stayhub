package com.stayhub.channel.controller;

import com.stayhub.account.dto.LoginUser;
import com.stayhub.account.entity.UserRole;
import com.stayhub.channel.dto.MockChannelFeed;
import com.stayhub.channel.service.MockChannelClient;
import com.stayhub.channel.service.MockReservationImportService;
import com.stayhub.common.ApiResponse;
import com.stayhub.common.ForbiddenException;
import com.stayhub.common.UnauthorizedException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/channel-sync")
@RequiredArgsConstructor
public class ChannelSyncController {
    private final MockChannelClient mockChannelClient;
    private final MockReservationImportService importer;

    @GetMapping("/mock-a/preview")
    public ApiResponse<List<MockChannelFeed.Booking>> previewChannelA(
            @SessionAttribute(name = LoginUser.SESSION_KEY, required = false) LoginUser user) {
        requireHost(user);
        return ApiResponse.success(mockChannelClient.readChannelA());
    }

    @PostMapping("/mock-a/import/{externalId}")
    public ApiResponse<MockReservationImportService.Result> importOne(
            @PathVariable("externalId") String externalId,
            @SessionAttribute(name = LoginUser.SESSION_KEY, required = false) LoginUser user) {
        requireHost(user);
        var feed = mockChannelClient.readChannelA();
        return ApiResponse.success(importer.importOne(externalId, feed));
    }

    private void requireHost(LoginUser user) {
        if (user == null) throw new UnauthorizedException("로그인이 필요합니다.");
        if (user.role() != UserRole.HOST)
            throw new ForbiddenException("호스트만 채널 연동 기능을 사용할 수 있습니다.");
    }
}
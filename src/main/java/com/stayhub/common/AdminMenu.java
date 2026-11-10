package com.stayhub.common;

import static com.stayhub.account.entity.UserRole.CLEANING;
import static com.stayhub.account.entity.UserRole.FRONT;
import static com.stayhub.account.entity.UserRole.HOST;
import static com.stayhub.account.entity.UserRole.STAFF;

import com.stayhub.account.entity.UserRole;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 관리자 화면 좌측 메뉴 목록 (사이트맵_IA 시트의 관리자 메뉴 중 "1차 범위 포함"만).
 *
 * <p><b>이 파일이 하는 일</b>
 * <br>메뉴 하나 = 값 하나. 1depth 메뉴 이름·화면 ID·2depth 메뉴 이름·주소·접근 역할을 적어 둡니다.
 * 레이아웃({@code templates/admin/layout.html})이 로그인 사용자의 역할로 {@link #groupsFor} 를 불러
 * 볼 수 있는 메뉴만 그립니다. 볼 수 없는 메뉴는 HTML 에 아예 나오지 않습니다(정책정의서 "메뉴 노출").
 * 접근 역할은 사이트맵의 "접근 권한" 칸을 그대로 옮겼고, HOST 는 칸과 상관없이 전부 봅니다(정책정의서 "호스트").
 *
 * <p>아직 화면이 없는 메뉴를 누르면 "준비 중" 화면이 뜹니다({@code AdminController#ready}).
 * 내 화면의 컨트롤러를 만들어 같은 주소를 연결하면 그때부터 내 화면이 뜹니다.
 *
 * <p><b>따라 만들 때 바꿀 곳</b>
 * <br>대부분 없습니다. 내 화면이 이미 여기 있는지 화면 ID 로 찾아보고, 주소만 확인하세요.
 * 주소를 바꾸면 내 컨트롤러의 {@code @GetMapping} 도 같은 값이어야 합니다.
 * 메뉴를 새로 넣거나 역할을 바꾸려면 사이트맵을 먼저 고쳐야 하니 총괄과 상의하세요.
 * 여기서 메뉴를 숨겨도 주소를 직접 치면 열립니다. 주소 차단(403)은 ACC-005 에서 추가합니다.
 */
public enum AdminMenu {

	// STAFF(관리 스태프)가 볼 메뉴는 ACC-005 에서 정합니다. 지금은 사이트맵대로 대시보드만.
	DASHBOARD("대시보드", "SCR-STA-01", "대시보드", "/admin", HOST, STAFF),

	RESERVATION_LIST("예약관리", "SCR-RSV-01", "예약 목록 / 통합 검색", "/admin/reservations", HOST, FRONT),
	// SCR-RSV-02 예약 상세는 목록에서 예약을 눌러 들어가는 화면이라(예약 ID 필요) 메뉴에 넣지 않습니다.
	RESERVATION_CREATE("예약관리", "SCR-RSV-03", "예약 등록", "/admin/reservations/new", HOST, FRONT),
	CHECK_IN_OUT("예약관리", "SCR-RSV-05", "체크인 / 체크아웃", "/admin/check-in-out", FRONT),
	SPECIAL_REQUEST("예약관리", "SCR-RSV-07", "특별 요청사항", "/admin/special-requests", FRONT, HOST),

	ROOM_BOARD("객실관리", "SCR-ROM-01", "객실 현황 보드", "/admin/rooms", HOST, FRONT, CLEANING),
	HOUSEKEEPING("객실관리", "SCR-ROM-05", "청소 스케줄", "/admin/housekeeping-tasks", CLEANING, FRONT),

	PAYMENT_LIST("결제관리", "SCR-PAY-01", "결제 / 환불 내역", "/admin/payments", HOST, FRONT),
	ADDITIONAL_CHARGE("결제관리", "SCR-PAY-02", "추가 요금 등록", "/admin/additional-charges", FRONT),
	CHANNEL_COMMISSION("결제관리", "SCR-PAY-03", "채널 수수료 설정", "/admin/channel-commissions", HOST),
	TAX_INVOICE("결제관리", "SCR-PAY-04", "영수증 / 세금계산서", "/admin/tax-invoices", HOST, FRONT),

	CHANNEL_SETTING("채널연동", "SCR-CHN-01", "채널 연동 설정", "/admin/channels", HOST),
	CHANNEL_SYNC_LOG("채널연동", "SCR-CHN-02", "동기화 로그", "/admin/channel-sync-logs", HOST),

	PROPERTY("콘텐츠관리", "SCR-CMS-01", "소개페이지 편집", "/admin/properties", HOST),
	GUIDEBOOK("콘텐츠관리", "SCR-CMS-02", "가이드북 관리", "/admin/guidebooks", HOST, FRONT),

	INBOX("커뮤니케이션", "SCR-COM-01", "통합 인박스", "/admin/messages", FRONT),

	STAFF_ACCOUNT("시스템관리", "SCR-ACC-04", "직원 계정 관리", "/admin/users", HOST);

	private final String group;
	private final String screenId;
	private final String label;
	private final String url;
	private final Set<UserRole> roles;

	AdminMenu(String group, String screenId, String label, String url, UserRole... roles) {
		this.group = group;
		this.screenId = screenId;
		this.label = label;
		this.url = url;
		this.roles = Set.of(roles);
	}

	/** 이 역할이 볼 수 있는 메뉴를 1depth 이름별로 묶어 정의 순서대로 돌려줍니다. */
	public static Map<String, List<AdminMenu>> groupsFor(UserRole role) {
		return Arrays.stream(values())
				.filter(menu -> role == HOST || menu.roles.contains(role))
				.collect(Collectors.groupingBy(menu -> menu.group, LinkedHashMap::new, Collectors.toList()));
	}

	/** 지금 보고 있는 주소가 이 메뉴(또는 그 아래 화면)이면 true. 메뉴를 강조 표시할 때 씁니다. */
	public boolean isActive(String requestUri) {
		return requestUri.equals(url) || (!this.equals(DASHBOARD) && requestUri.startsWith(url + "/"));
	}

	public String getScreenId() {
		return screenId;
	}

	public String getLabel() {
		return label;
	}

	public String getUrl() {
		return url;
	}
}

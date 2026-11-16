package com.stayhub.content;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.stayhub.common.PropertyScope;
import com.stayhub.content.entity.Guidebook;
import com.stayhub.content.entity.GuidebookCategory;
import com.stayhub.content.property.entity.PropertyFacility;
import com.stayhub.content.property.entity.PropertyImage;
import com.stayhub.content.property.repository.PropertyFacilityRepository;
import com.stayhub.content.property.repository.PropertyImageRepository;
import com.stayhub.content.repository.GuidebookRepository;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * 게스트 조회 전용 API·화면 테스트 (SCR-GST-01·02).
 *
 * <p>모든 요청은 로그인 없이(세션 없이) 보냅니다.
 * {@code @Transactional} 이라 테스트가 끝나면 넣었던 데이터가 전부 되돌려집니다(롤백).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PublicContentApiTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private PropertyFacilityRepository propertyFacilityRepository;

	@Autowired
	private PropertyImageRepository propertyImageRepository;

	@Autowired
	private GuidebookRepository guidebookRepository;

	@BeforeEach
	void setUp() {
		// 경고: @Transactional을 지우면 개발 DB의 이 테이블들이 실제로 비워집니다. 지우지 마세요.
		propertyFacilityRepository.deleteAll();
		propertyImageRepository.deleteAll();
		guidebookRepository.deleteAll();
	}

	/** 게스트 화면은 property_id = 1 만 봅니다. 엔티티로 만들면 AUTO_INCREMENT 번호가 붙어서 SQL 로 1번을 직접 넣습니다. */
	private void saveProperty(String status) {
		jdbcTemplate.update("""
				INSERT INTO properties (property_id, property_name, description, status, created_at, updated_at)
				VALUES (?, '바다 앞 펜션', ?, ?, NOW(), NOW())
				ON DUPLICATE KEY UPDATE property_name = VALUES(property_name),
				  description = VALUES(description), status = VALUES(status)
				""", PropertyScope.DEFAULT_PROPERTY_ID, "<b>바다</b>가 보입니다\n조용합니다", status);
	}

	@Test
	@DisplayName("로그인 없이 숙소 소개가 200 이고, 시설은 시설 순서, 사진은 노출 순서, 내부 값은 없다")
	void publishedProperty() throws Exception {
		saveProperty("PUBLISHED");
		// data.sql 의 시설 순서: PARKING(1) → WIFI(2) → BREAKFAST(3). 체크는 WIFI·PARKING 만
		propertyFacilityRepository.save(PropertyFacility.create(PropertyScope.DEFAULT_PROPERTY_ID, "WIFI"));
		propertyFacilityRepository.save(PropertyFacility.create(PropertyScope.DEFAULT_PROPERTY_ID, "PARKING"));
		propertyImageRepository.save(PropertyImage.create(PropertyScope.DEFAULT_PROPERTY_ID, "/img/b.jpg", 100, "jpg", false, 2));
		propertyImageRepository.save(PropertyImage.create(PropertyScope.DEFAULT_PROPERTY_ID, "/img/a.jpg", 100, "jpg", true, 1));

		mockMvc.perform(get("/api/public/property"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.propertyName").value("바다 앞 펜션"))
				.andExpect(jsonPath("$.data.description").value("<b>바다</b>가 보입니다\n조용합니다"))
				.andExpect(jsonPath("$.data.facilities").value(Matchers.contains("주차", "와이파이")))
				.andExpect(jsonPath("$.data.images").value(Matchers.contains("/img/a.jpg", "/img/b.jpg")))
				.andExpect(jsonPath("$.data.status").doesNotExist())
				.andExpect(jsonPath("$.data.id").doesNotExist());
	}

	@Test
	@DisplayName("DRAFT 숙소면 '준비 중인 숙소입니다.'")
	void draftPropertyIsNotReady() throws Exception {
		saveProperty("DRAFT");

		mockMvc.perform(get("/api/public/property"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("준비 중인 숙소입니다."));
	}

	@Test
	@DisplayName("로그인 없이 가이드북이 200 이고, ACTIVE 만 노출 순서대로, 카테고리 필터, 내부 값 없음")
	void guidebooks() throws Exception {
		saveProperty("PUBLISHED");
		guidebookRepository.save(Guidebook.create(PropertyScope.DEFAULT_PROPERTY_ID, GuidebookCategory.WIFI, "비밀번호", "1234", 2));
		guidebookRepository.save(Guidebook.create(PropertyScope.DEFAULT_PROPERTY_ID, GuidebookCategory.PARKING, "주차장", "건물 뒤", 1));
		Guidebook deleted = Guidebook.create(PropertyScope.DEFAULT_PROPERTY_ID, GuidebookCategory.WIFI, "삭제된 안내", "x", 0);
		deleted.delete();
		guidebookRepository.save(deleted);

		mockMvc.perform(get("/api/public/guidebooks"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[*].title").value(Matchers.contains("주차장", "비밀번호")))
				.andExpect(jsonPath("$.data[0].category").value("PARKING"))
				.andExpect(jsonPath("$.data[0].content").value("건물 뒤"))
				.andExpect(jsonPath("$.data[0].sortOrder").value(1))
				.andExpect(jsonPath("$.data[0].id").doesNotExist())
				.andExpect(jsonPath("$.data[0].status").doesNotExist())
				.andExpect(jsonPath("$.data[0].propertyId").doesNotExist());

		mockMvc.perform(get("/api/public/guidebooks").param("category", "WIFI"))
				.andExpect(jsonPath("$.data[*].title").value(Matchers.contains("비밀번호")));
	}

	@Test
	@DisplayName("숙소가 DRAFT 면 가이드북도 '준비 중인 숙소입니다.'")
	void draftPropertyBlocksGuidebooks() throws Exception {
		saveProperty("DRAFT");
		guidebookRepository.save(Guidebook.create(PropertyScope.DEFAULT_PROPERTY_ID, GuidebookCategory.WIFI, "비밀번호", "1234", 1));

		mockMvc.perform(get("/api/public/guidebooks"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("준비 중인 숙소입니다."));
	}

	@Test
	@DisplayName("조회 전용: 로그인 없이 POST /api/public/guidebooks 는 405")
	void postIsNotAllowed() throws Exception {
		mockMvc.perform(post("/api/public/guidebooks")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"category\":\"WIFI\",\"title\":\"t\",\"content\":\"c\"}"))
				.andExpect(status().isMethodNotAllowed());
	}

	@Test
	@DisplayName("기존 보호 유지: 로그인 없이 /api/guidebooks 는 401, /admin 은 /login?expired")
	void existingProtectionStays() throws Exception {
		mockMvc.perform(get("/api/guidebooks"))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(get("/admin"))
				.andExpect(redirectedUrl("/login?expired"));
	}

	@Test
	@DisplayName("로그인 없이 / 와 /guidebook 화면이 200")
	void guestPagesAreOpen() throws Exception {
		mockMvc.perform(get("/"))
				.andExpect(status().isOk())
				.andExpect(view().name("guest/intro"))
				.andExpect(content().string(Matchers.containsString("관리자 로그인")));
		mockMvc.perform(get("/guidebook"))
				.andExpect(status().isOk())
				.andExpect(view().name("guest/guidebook"));
	}
}

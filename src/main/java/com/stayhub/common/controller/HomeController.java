package com.stayhub.common.controller;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 개발 환경 세팅이 끝났는지 눈으로 확인하는 화면.
 *
 * <p>팀원이 프로젝트를 처음 실행했을 때 흰 에러 화면 대신
 * "성공했다"는 걸 바로 알 수 있게 하려고 만들었습니다.
 * 실제 서비스 화면이 아니므로 나중에 지워도 됩니다.
 */
@Controller
public class HomeController {

	private static final Logger log = LoggerFactory.getLogger(HomeController.class);
	private static final DateTimeFormatter TIME_FORMAT =
			DateTimeFormatter.ofPattern("yyyy년 M월 d일 HH:mm:ss");

	private final DataSource dataSource;

	public HomeController(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	@GetMapping("/")
	public String home(Model model) {
		model.addAttribute("currentTime", LocalDateTime.now().format(TIME_FORMAT));
		addDatabaseStatus(model);
		return "index";
	}

	/**
	 * DB에 실제로 접속해 보고 결과를 화면에 넘깁니다.
	 *
	 * <p>연결에 실패해도 예외를 밖으로 던지지 않습니다. 에러 화면이 떠버리면
	 * 팀원이 무엇이 잘못됐는지 알 수 없으니, 화면 안에서 실패 사유를 보여주는 편이 낫습니다.
	 */
	private void addDatabaseStatus(Model model) {
		try (Connection connection = dataSource.getConnection()) {
			DatabaseMetaData metaData = connection.getMetaData();
			model.addAttribute("dbConnected", true);
			model.addAttribute("dbDetail",
					metaData.getDatabaseProductName() + " " + metaData.getDatabaseProductVersion()
							+ " · 스키마 " + connection.getCatalog());
		} catch (Exception e) {
			log.error("DB 연결 확인 실패", e);
			model.addAttribute("dbConnected", false);
			model.addAttribute("dbDetail", e.getMessage());
		}
	}
}

# stayhub

호텔 CMS(웹사이트 콘텐츠 관리) + PMS(객실·예약 관리) 통합 관리 시스템

- **기간** 2026.09 ~ 12 (개발 마감 11월 말, 최종 제출 12/25)
- **팀** 5명 · 창업 동아리 팀 프로젝트
- **정기 회의** 매주 화요일 30분 (제출물은 전날 월요일까지)

## 담당

| 담당 | 영역 | 브랜치 코드 |
| --- | --- | --- |
| 팀원 A | 계정·통계·시스템 | `acc` `sta` `sys` |
| 팀원 B | 예약·채널 | `rsv` `chn` |
| 팀원 C | 객실·결제 | `rom` `pay` |
| 팀원 D | 화면 퍼블리싱 | `pub` |
| 총괄 | 콘텐츠·커뮤니케이션 | `cms` `com` |

## 폴더 구조

- `docs/practice/` — 첫 PR 연습용
- `publishing/` — 정적 화면 (HTML/CSS)
- 루트 — Spring Boot 프로젝트 (10월 중 생성 예정)

## 개발 규칙

- **브랜치** `main` / `feature-{코드}-{작업명}` — 예: `feature-rsv-checkin`
- **커밋** `[도메인] 작업내용` — 예: `[RSV] 예약 목록 검색 조건 추가`
- **병합** main 직접 push 금지. feature → PR → 총괄 승인 → Squash merge → 브랜치 삭제
- **PR 본문** 무엇을 / 왜 / 확인한 것 / 관련 기능ID 4항목 필수. 변경 파일 20개 이하
- **금지** main 직접 push · `git push -f` · 남의 도메인 파일 임의 수정
- **커밋 제외** DB 접속정보, `application-local.yml`, 빌드 결과물, 10MB 초과 파일

## 막혔을 때

같은 문제로 **30분**을 넘겼으면 바로 총괄에게 연락하세요. 혼자 세 시간 붙잡는 것이 팀에 더 손해입니다. 연락할 때 아래 세 가지를 함께 보내주세요.

1. 실행한 명령어
2. 에러 메시지 전문 (스크린샷보다 텍스트)
3. `git status` 출력

## 가이드

- 팀 Git 매뉴얼 — <링크>
- 5인 팀 작업 분담표 — <링크>
- 주간 회의록 양식 — <링크>
- 화면 퍼블리싱 4주 로드맵 (팀원 D 전용) — <링크>

## 기술 스택

Java / Spring Boot / JPA / MySQL / Thymeleaf
※ 구체적인 버전은 9/22 회의에서 확정 (팀원 A 제안)

## 실행 방법

처음 받았다면 아래 3단계를 순서대로 하세요. 끝나면 브라우저에 성공 화면이 뜹니다.

### 1. 데이터베이스 만들기

MySQL에 접속해 `stayhub` 데이터베이스를 만듭니다.

```sql
CREATE DATABASE stayhub DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

테이블은 만들지 않아도 됩니다. 애플리케이션이 실행되면서 자동으로 생성합니다.

### 2. application-local.yml 작성하기

`src/main/resources/application-local.yml.example` 파일을 같은 폴더에 복사한 뒤,
이름을 `application-local.yml` 로 바꾸고 비밀번호를 본인 것으로 채웁니다.

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/stayhub?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
    username: root
    password: 본인이 MySQL 설치할 때 정한 비밀번호
```

이 파일은 `.gitignore` 에 등록돼 있어 커밋되지 않습니다.
**이 단계를 건너뛰면 실행할 때 `Failed to configure a DataSource` 오류가 납니다.**

### 3. 실행하기

```bash
./gradlew bootRun
```

Windows 명령 프롬프트에서는 `gradlew bootRun` 입니다.
처음 실행하면 Gradle과 라이브러리를 내려받느라 몇 분 걸립니다.

실행된 뒤 브라우저에서 <http://localhost:8080> 을 열어
**"stayhub 개발 환경 세팅 완료"** 화면과 **DB 연결: 성공** 이 보이면 끝입니다.

DB 연결이 실패로 나오면 화면에 확인할 항목이 함께 표시됩니다.
30분 넘게 막히면 총괄에게 연락하세요.

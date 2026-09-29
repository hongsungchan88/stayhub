# 새 기능 만드는 법

숙소 가이드북(CMS-002)이 **참고 구현**입니다. 테이블 하나에 등록·조회·수정(유형에 따라 삭제)을 만드는 기능은
전부 가이드북과 같은 모양으로 만듭니다. 새로운 방식을 고민하지 말고, 가이드북을 베끼세요.

모양이 모두 같아야 서로의 코드를 바로 읽을 수 있고, 리뷰할 때 "가이드북과 다른 곳"만 보면 됩니다.

## 순서

### ① main 최신으로 받고 브랜치 만들기

```
git switch main
git pull
git switch -c feature-{코드}-{작업명}
```

예) 객실 기능이면 `feature-rom-room`. 코드는 README "## 담당" 표를 보세요.

### 먼저 — 내 테이블이 어느 유형인지 확인하기

가이드북은 **삭제 기능이 있는 테이블**입니다. 34개 테이블 중 이런 테이블은 2개뿐이라,
대부분은 가이드북에서 몇 가지를 빼고 만듭니다. 테이블 정의서의 `status` 컬럼을 보고 유형을 고르세요.

| 유형 | `status` 컬럼 | 해당 테이블 |
| --- | --- | --- |
| ① 삭제 기능 있음 | `ACTIVE` / `DELETED` (소프트 삭제) | users, guidebooks (2개) |
| ② 업무 상태 있음 | 예약 상태처럼 업무 흐름을 나타내는 값 | rooms, reservations, housekeeping_tasks, special_requests, properties, promotions (6개) |
| ③ status 없음 | 없음 | 나머지 26개 (channels, payments, property_images 등) |

유형별로 가이드북에서 뺄 것

- **① 삭제 기능 있음** — 가이드북 그대로 만듭니다.
- **② 업무 상태 있음** — `EntityStatus` 대신 도메인 enum 을 새로 만듭니다 (예: `RoomStatus`. 규약 2번대로 STRING + VARCHAR).
  조회에 status 조건 없이 `findAll(pageable)`·`findById` 를 씁니다.
  `delete()`·DELETE API·`deleteRemovesFromList` 테스트는 만들지 않습니다.
  `getDeletedThrows` 는 delete 줄을 지우고 `created.id()` 대신 `99999L` 로 조회하는 "없는 항목 조회" 테스트로 바꿔 남깁니다.
- **③ status 없음** — status 관련 줄을 전부 지웁니다. `findAll(pageable)`·`findById` 를 씁니다. 삭제 기능은 없습니다.

②·③은 `JpaRepository` 에 이미 있는 기본 메서드(`findAll`, `findById`, `save`)로 충분해서,
필터 조건이 없다면 Repository 에 메서드를 하나도 선언하지 않아도 됩니다.
가이드북 코드의 `// ★ 바꿀 곳:` 주석에도 "유형 ②·③이면 삭제"처럼 유형별 안내가 적혀 있습니다.

**내 기능에 삭제가 필요해 보이면 만들기 전에 총괄에게 묻는다.**
1차 기능 중 삭제가 있는 건 ACC-005·CMS-002 둘뿐입니다(정책정의서 24행).

### ② 가이드북 파일 열어두기

아래 파일을 IntelliJ에서 열어두세요. 경로는 모두 `src/main/java/com/stayhub/` 아래입니다.

| 파일 | 하는 일 | 복사해서 내 것으로 |
| --- | --- | --- |
| `content/entity/Guidebook.java` | 테이블 한 행 | O |
| `content/entity/GuidebookCategory.java` | enum 컬럼의 값 목록 | enum 컬럼이 있을 때만 |
| `content/repository/GuidebookRepository.java` | DB 조회 | O |
| `content/service/GuidebookService.java` | 업무 로직 | O |
| `content/controller/GuidebookController.java` | API 주소 | O |
| `content/dto/GuidebookCreateRequest.java` | 등록 요청 | O |
| `content/dto/GuidebookUpdateRequest.java` | 수정 요청 | O |
| `content/dto/GuidebookResponse.java` | 응답 | O |
| `src/test/java/com/stayhub/content/GuidebookServiceTest.java` | 테스트 | O |

**`common/` 폴더 파일은 복사하지 마세요.** `ApiResponse`, `BusinessException`, `BaseEntity`,
`EntityStatus`, `PageResponse`, `PropertyScope` 는 모든 기능이 같이 쓰는 파일이라 그대로 import 합니다.
`EntityStatus` 는 유형 ①만, `PropertyScope` 는 property_id 가 있는 4개 테이블만 씁니다.

각 파일에서 바꿔야 하는 줄에는 `// ★ 바꿀 곳:` 주석이 붙어 있습니다.
IntelliJ에서 `Ctrl + Shift + F` → `★ 바꿀 곳` 으로 검색하면 한 번에 모아볼 수 있습니다.

가이드북에만 있는 규칙이 두 가지 있습니다. 내 테이블에 없으면 지우세요.
- **중복 제목 경고** — 같은 카테고리에 같은 제목이 있으면 저장은 하되 경고 메시지를 붙입니다
- **카테고리 필터** — 목록을 카테고리로 거릅니다

### ③ AI에게 요청하기

아래 틀을 그대로 복사해서 `[ ]` 부분만 채워 넣으세요.

```
content 도메인의 Guidebook 관련 파일들을 참고해서, 똑같은 구조로
[테이블명] 기능을 [도메인 폴더]에 만들어줘.
테이블 정의: [테이블 정의서 행 붙여넣기]
이 테이블은 [유형 ① 삭제 기능 있음 / ② 업무 상태 있음 / ③ status 없음]이야.
②·③이면 EntityStatus·status 조회 조건·delete()·DELETE API·삭제 테스트는 빼되, 없는 항목 조회 테스트는 99999L 로 조회하도록 바꿔서 남겨줘.
- 다른 테이블을 가리키는 컬럼은 Long ID 값으로만 (@ManyToOne 금지)
- 컬럼·테이블 이름은 테이블 정의서와 한 글자도 다르지 않게
- 가이드북에 없는 새로운 방식은 쓰지 말 것
```

예) `[테이블명]` → `rooms`, `[도메인 폴더]` → `room`,
`[테이블 정의서 행 붙여넣기]` → 테이블 정의서의 rooms 테이블 행을 통째로 복사,
`[유형 …]` → `② 업무 상태 있음` (rooms 는 유형 ②)

### ④ 가이드북과 나란히 비교하기

AI가 만든 파일과 가이드북 파일을 하나씩 나란히 놓고 봅니다.
IntelliJ에서 두 파일을 `Ctrl` 을 누른 채 함께 선택 → `Ctrl + D` 를 누르면 좌우로 비교됩니다.

이런 게 보이면 고쳐야 합니다.
- 가이드북에 없는 어노테이션 — `@Setter`, `@Data`, `@Builder`, `@ManyToOne`, `@Lob` 등
- `// ★ 바꿀 곳:` 줄인데 가이드북 이름이 그대로 남아 있음 — `Guidebook`, `guidebook`, `가이드북` 으로 검색
- 컬럼명·테이블명이 테이블 정의서와 한 글자라도 다름
- enum 컬럼에 `columnDefinition = "VARCHAR(n)"` 이 빠짐
- 유형 ②·③인데 `EntityStatus`·`delete()`·`@DeleteMapping` 이 남아 있음

### ⑤ 실행해서 확인하기

1. **테스트** — IntelliJ에서 테스트 클래스 이름 왼쪽 초록 화살표, 또는 터미널에서 `./gradlew test`.
   MySQL이 켜져 있어야 합니다.
2. **테이블 확인** — 앱을 실행하면 테이블이 자동으로 만들어집니다. MySQL에서 아래를 실행해
   테이블 정의서와 컬럼 이름·타입을 한 줄씩 대조하세요. 특히 enum 컬럼이 `varchar` 인지 확인합니다.
   ```
   SHOW CREATE TABLE 내테이블명;
   ```
3. **API 호출** — 목록·상세는 브라우저 주소창에 바로 넣어보면 됩니다.
   예) <http://localhost:8080/api/guidebooks>
   등록·수정·삭제(유형 ①만)는 Git Bash에서 curl로 확인합니다.
   ```
   curl -X POST http://localhost:8080/api/guidebooks -H "Content-Type: application/json" -d '{"category":"WIFI","title":"와이파이","content":"비밀번호 1234"}'
   curl -X PUT http://localhost:8080/api/guidebooks/1 -H "Content-Type: application/json" -d '{"category":"WIFI","title":"와이파이 안내","content":"비밀번호 5678"}'
   curl -X DELETE http://localhost:8080/api/guidebooks/1
   ```
   응답의 `"success":true` 를 확인하세요. 없는 ID(예: 99999)로 상세 조회하면 `"success":false` 가 나와야 정상입니다.
   유형 ①은 삭제한 뒤 같은 ID로 상세 조회해도 `"success":false` 가 나와야 합니다.

막히면 [troubleshooting.md](troubleshooting.md)를 먼저 보세요.

### ⑥ PR 올리기

1. 커밋 메시지는 `[도메인] 작업내용` — 예) `[ROM] 객실 등록·조회·수정 추가`
2. push 한 뒤 GitHub에서 main 으로 향하는 PR을 만듭니다
3. PR 본문에 **무엇을 / 왜 / 확인한 것 / 관련 기능ID** 네 항목을 씁니다.
   "확인한 것"에는 ⑤에서 해본 것(테스트 통과, `SHOW CREATE TABLE` 대조, API 호출 결과)을 적으세요
4. 변경 파일은 20개 이하

## 규약 체크리스트

PR을 올리기 전에 하나씩 체크하세요. 가이드북은 전부 지키고 있습니다.

- [ ] **1. `@Table(name = "복수형")` 명시, `@Column(name = "...")` 으로 컬럼명 전부 명시**
  — 빼면 테이블명이 클래스명(단수형)으로 만들어져 정의서와 달라지고, 적어두면 정의서와 한 줄씩 대조할 수 있습니다.
- [ ] **2. enum 은 `@Enumerated(EnumType.STRING)` + `columnDefinition = "VARCHAR(n)"`**
  — 기본값(ORDINAL)은 0·1·2 순번으로 저장돼 값을 중간에 추가하면 기존 데이터 뜻이 바뀌고, VARCHAR 를 빼면 MySQL ENUM 타입이 되어 값을 추가할 때 저장이 실패합니다.
- [ ] **3. 다른 테이블은 `@ManyToOne` 대신 `Long xxxId` 값으로만. `property_id` 는 등록할 때 서비스에서 `PropertyScope.DEFAULT_PROPERTY_ID` 를 넣고, 요청에서 받지 않으며, 조회 조건에는 넣지 않는다 (property_id 가 있는 4개 테이블만 해당 — rooms, property_images, property_facilities, guidebooks. properties는 property_id가 자기 PK(AUTO_INCREMENT)라 해당 없음. PropertyScope를 넣지 않는다)**
  — `open-in-view: false` 라서 연관관계는 서비스 밖에서 지연 로딩 예외가 나고, 1차는 숙소가 하나라 조회에 숙소 조건을 걸지 않되 컬럼은 정의해 둡니다(정책정의서 32행).
- [ ] **4. Lombok 은 `@Getter`, `@NoArgsConstructor(access = AccessLevel.PROTECTED)` 만. 생성은 `create(...)`, 변경은 `update(...)`, 삭제는 `delete()` — `delete()` 는 유형 ①만**
  — `@Setter` 가 있으면 어디서 값이 바뀌었는지 추적이 안 되고 `@Builder` 는 필수값을 빠뜨려도 컴파일되니, 값이 바뀌는 곳을 이 메서드들로 모아둡니다.
- [ ] **5. 컨트롤러는 엔티티를 반환하지 않는다. 응답 DTO 변환은 서비스 안에서**
  — 엔티티를 그대로 내보내면 내부 값이 새어 나가고 컬럼을 추가할 때마다 API 응답이 같이 바뀌어 화면이 깨집니다.
- [ ] **6. 서비스 클래스에 `@Transactional(readOnly = true)`, 쓰기 메서드에만 `@Transactional`**
  — 조회 메서드에서 실수로 값을 바꿔도 DB에 반영되지 않고, 어떤 메서드가 DB를 바꾸는지 한눈에 보입니다.
- [ ] **7. 응답은 전부 `ApiResponse` 로 감싼다**
  — 성공이든 실패든 응답 모양이 하나라서 화면 쪽은 한 가지 방식으로만 처리하면 됩니다.
- [ ] **8. 없는 항목은 `throw new BusinessException("○○을 찾을 수 없습니다.")` — DELETED 를 없는 것으로 보는 건 유형 ①만**
  — 유형 ①에서 삭제된 항목은 사용자에게 없는 것과 같고, 이 메시지는 그대로 화면에 표시되니 사용자가 읽을 문장으로 씁니다.

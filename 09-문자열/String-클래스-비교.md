## String 클래스 비교

문자열은 자바에서 가장 많이 쓰는 타입이면서, **가장 자주 성능 문제를 만드는 타입**이기도 하다.
`String`이 불변이라는 사실 하나에서 String Pool, `==` 함정, 반복문 안의 `+` 문제, `StringBuilder`가 존재하는 이유가 전부 파생된다.
여기서는 세 클래스의 차이를 정리하고, **왜 그렇게 설계됐고 언제 무엇을 골라야 하는지**까지 다룬다.

### String은 왜 불변인가
+ `String`은 `final` 클래스이고 내부 배열도 `final`이라, 한 번 만들어지면 내용이 바뀌지 않음
    - Java 8까지 — `private final char[] value`
    - Java 9+ — `private final byte[] value` + `byte coder` (Compact Strings, JEP 254)
        + ASCII만 담긴 문자열은 1바이트씩 저장 → 같은 문자열이 메모리를 절반만 씀
    - `final` 클래스라 상속해서 동작을 바꿀 수도 없음
+ 불변으로 만든 이유
    - **String Pool 공유가 가능해짐** — 같은 리터럴을 여러 곳이 나눠 써도 서로 영향이 없음
    - **스레드 안전** — 여러 스레드가 동시에 읽어도 동기화가 필요 없음
    - **`hashCode()` 캐싱** — 값이 변하지 않으므로 한 번 계산해서 `hash` 필드에 저장
        + `HashMap`의 키로 `String`이 특히 빠른 이유
    - **보안** — 파일 경로·DB URL·클래스 이름을 검사한 뒤에 값이 바뀌지 않음이 보장됨
        + 가변이었다면 "검사 통과 후 값 교체" 공격이 성립함
+ 대가는 **바꿀 때마다 새 객체**

    ```java
    String str = "Hello";
    str += " World";   // 기존 객체를 고치는 게 아니라, 새 객체를 만들어 참조를 옮김
    ```

    - "Hello"는 그대로 남아 있고, `str`이 새로 만든 "Hello World"를 가리키게 될 뿐
    - 이 한 줄이 반복문에 들어가는 순간 문제가 됨 → [문자열 연결의 진짜 비용](#문자열-연결의-진짜-비용)

### String Pool과 리터럴
+ **String Pool** — JVM이 문자열 리터럴을 한 곳에 모아 재사용하는 캐시
    - Java 7부터 PermGen이 아니라 **힙**에 있음 (그래서 GC 대상이 되고 크기 제약도 사라짐)
    - 리터럴은 클래스 로딩 시점에 풀에 등록되고, 같은 값이면 **같은 객체**를 돌려줌

    ```java
    String a = "hello";
    String b = "hello";
    System.out.println(a == b);            // true  ← 풀에서 같은 객체

    String c = new String("hello");
    System.out.println(a == c);            // false ← new는 무조건 힙에 새 객체
    System.out.println(a.equals(c));       // true
    ```

+ **컴파일 타임에 값이 정해지면 미리 합쳐짐** (상수 폴딩)

    ```java
    String s1 = "hel" + "lo";              // 컴파일 시점에 "hello"로 합쳐짐
    System.out.println(s1 == "hello");     // true

    String part = "hel";
    String s2 = part + "lo";               // 런타임 조합 → 풀에 들어가지 않음
    System.out.println(s2 == "hello");     // false

    final String fixed = "hel";            // 컴파일 타임 상수
    String s3 = fixed + "lo";              // 상수 폴딩 대상
    System.out.println(s3 == "hello");     // true
    ```

    - `final` 하나 붙이고 안 붙이고에 따라 결과가 뒤집힘 → **`==`로 문자열을 비교하면 안 되는 이유**
+ **`intern()`** — 런타임에 만든 문자열을 풀에 등록하고 대표 객체를 돌려받음

    ```java
    String s = new String("hello").intern();
    System.out.println(s == "hello");      // true
    ```

    - 남용하면 오히려 손해 — 풀은 고정 크기 해시 테이블(`-XX:StringTableSize`)이라 커지면 조회가 느려지고, `intern()` 호출 자체도 공짜가 아님
    - 같은 값이 대량으로 중복되는 경우가 아니면 쓸 일이 거의 없음
    - JVM 옵션 `-XX:+UseStringDeduplication`(G1)은 개발자가 손대지 않아도 GC가 중복 배열을 합쳐줌
+ ⚠️ `new String("hello")`는 **객체를 두 개 만듦** — 풀의 "hello"와 힙의 복사본
    - 그래서 실무에서 `new String(...)`을 쓸 일은 사실상 없음

### 문자열 연결의 진짜 비용
+ `+` 연산자 자체는 느리지 않음. 컴파일러가 최적화해 줌
    - Java 8까지 — javac가 `StringBuilder.append()` 체인으로 바꿔줌
    - Java 9+ — `invokedynamic` + `StringConcatFactory` (JEP 280)로 런타임에 최적 코드를 생성

    ```java
    String msg = "id=" + id + ", name=" + name;   // 한 줄짜리는 그대로 써도 됨
    ```

+ 문제는 **반복문 안에서 결과를 다시 이어붙일 때**

    ```java
    String result = "";
    for (int i = 0; i < 10000; i++) {
        result += i;     // ❌ 매 반복마다 새로 조립하고 새 String을 만듦
    }
    ```

    - 컴파일러 최적화는 **한 표현식 안에서만** 적용됨
    - 반복마다 지금까지 만든 문자열 전체를 다시 복사 → 연산량이 **O(n²)**
    - 10,000번이면 대략 5,000만 문자 복사. 문자열이 길수록 급격히 나빠짐

    ```java
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < 10000; i++) {
        sb.append(i);    // ✅ 같은 버퍼에 이어 씀 → O(n)
    }
    String result = sb.toString();
    ```

+ 정리하면 — **한 번에 이어붙이면 `+`, 나눠서 쌓아 올리면 `StringBuilder`**

### String vs StringBuffer vs StringBuilder
+ String
    - 불변 객체
    - 문자열이 변경될 경우 새로운 객체가 생성됨

    ```java
    String str = "Hello";
    str += " World"; // 기존 문자열 변경 불가 → 새 객체 생성
    ```

+ StringBuffer
    - 가변 객체, 멀티스레드 환경
    - 문자열을 변경할 수 있으며, 모든 public 메서드에 `synchronized`가 붙어 있음
        + 여러 스레드가 같은 인스턴스를 공유해도 내부 상태가 깨지지 않음
    - Java 1.0부터 있던 클래스라 오래된 코드에서 많이 보임

    ```java
    StringBuffer sb = new StringBuffer("Hello");
    sb.append(" World"); // 기존 객체에서 변경 (새 객체 생성 X)
    ```

+ StringBuilder
    - 가변 객체, 단일 스레드 환경
    - StringBuffer와 API가 완전히 동일하지만 동기화하지 않음
        + 락 획득·해제 비용이 없어 더 빠름
    - Java 5에서 추가됐고, **오늘날의 기본 선택지**

    ```java
    StringBuilder sb = new StringBuilder("Hello");
    sb.append(" World"); // 기존 객체에서 변경됨
    ```

+ 한 표로 비교

    | | String | StringBuffer | StringBuilder |
    |---|:--:|:--:|:--:|
    | 가변성 | 불변 | 가변 | 가변 |
    | 동기화 | 필요 없음(불변) | ✅ `synchronized` | ❌ |
    | 속도 | 조합이 많으면 느림 | 보통 | 빠름 |
    | 등장 | 1.0 | 1.0 | 5 |
    | 스레드 안전 | ✅ | ✅ (개별 연산만) | ❌ |
    | 쓰는 자리 | 값이 안 바뀌는 문자열 | 인스턴스를 스레드가 공유할 때 | 나머지 전부 |

+ 셋 다 `CharSequence`를 구현하므로, 읽기만 하는 메서드 파라미터는 `CharSequence`로 받으면 셋을 모두 받을 수 있음

### StringBuilder 제대로 쓰기
+ 자주 쓰는 메서드 — 대부분 `this`를 반환해서 체이닝됨

    ```java
    StringBuilder sb = new StringBuilder();
    sb.append("Hello").append(' ').append(42);   // "Hello 42"
    sb.insert(0, ">> ");                         // ">> Hello 42"
    sb.replace(0, 2, "##");                      // "## Hello 42"
    sb.delete(0, 3);                             // "Hello 42"
    sb.deleteCharAt(5);                          // "Hello42"
    sb.reverse();                                // "24olleH"
    sb.setLength(0);                             // 내용 비우기 (버퍼는 유지)
    ```

+ **초기 용량(capacity)을 지정하면 배열 복사를 줄일 수 있음**
    - 기본 용량은 16, `new StringBuilder(String)`은 `문자열 길이 + 16`
    - 넘치면 `(기존 용량 × 2) + 2`로 늘리면서 **배열을 통째로 복사**
    - 결과 크기를 대충이라도 알면 미리 잡아두는 게 좋음

    ```java
    StringBuilder sb = new StringBuilder(1024);
    ```

+ ⚠️ **`equals()`가 재정의되어 있지 않음**

    ```java
    new StringBuilder("a").equals(new StringBuilder("a"));   // ❌ false (주소 비교)
    new StringBuilder("a").toString().equals("a");           // ✅ true
    ```

    - `Set`이나 `Map`의 키로 쓰면 안 되는 이유
    - 내용 비교는 `toString()` 후 `equals()`, 또는 `compareTo()` (Java 11+)
+ ⚠️ `sb.length()`는 **현재 내용 길이**, `sb.capacity()`는 **버퍼 크기**로 서로 다름
+ 재사용이 필요하면 새로 만들지 말고 `setLength(0)`
    - 이미 늘려둔 버퍼를 그대로 쓰므로 재할당이 없음

### StringBuffer도 완전히 안전하지는 않다
+ `synchronized`가 보장하는 건 **메서드 하나가 원자적으로 실행된다**는 것뿐

    ```java
    if (buffer.length() == 0) {   // ← 이 사이에 다른 스레드가 끼어들 수 있음
        buffer.append("start");
    }
    ```

    - 여러 연산을 묶은 복합 동작은 여전히 직접 동기화해야 함
+ 실제로는 **`StringBuffer`를 스레드끼리 공유하는 설계 자체가 드묾**
    - 대부분 메서드 안에서 만들고 그 안에서 끝나는 지역 변수
    - 이 경우 JIT가 락을 아예 제거(lock elision)하기도 하지만, 의도를 드러내는 쪽은 `StringBuilder`
+ 결론 — **기본은 `StringBuilder`, 공유되는 게 확실할 때만 `StringBuffer`**
    - 더 나은 답은 애초에 가변 객체를 공유하지 않는 것

### 문자열 비교
+ 값 비교는 **항상 `equals()`** — `==`는 같은 객체인지를 묻는 것이라 우연히 true가 될 뿐

    ```java
    "hello".equals(input)              // NPE 안전 — 리터럴을 앞에 둠
    Objects.equals(a, b)               // 양쪽 다 null일 수 있을 때
    a.equalsIgnoreCase(b)              // 대소문자 무시
    ```

+ 순서 비교는 `compareTo()` — UTF-16 코드 유닛 기준의 사전순
    - `"apple".compareTo("banana")` → 음수
    - ⚠️ 사람이 기대하는 순서와 다름 — 대문자가 소문자보다 먼저 오고, 악센트나 한글 자모 정렬도 로케일을 반영하지 않음
    - 사람이 볼 정렬은 `Collator`를 쓰는 게 맞음
+ ⚠️ **대소문자 변환은 로케일을 탐**

    ```java
    "TITLE".toLowerCase();               // 터키 로케일에서는 "tıtle"
    "TITLE".toLowerCase(Locale.ROOT);    // ✅ 항상 "title"
    ```

    - 사람에게 보여주는 게 아니라 **비교·키 생성 목적이면 반드시 `Locale.ROOT`**
+ 더 자세한 판정 규칙은 [==와 equals()의 차이](==와-equals\(\)의-차이.md) 문서에서 다룸

### 자주 쓰는 메서드
+ 기본

    | 메서드 | 설명 | 비고 |
    |---|---|---|
    | `length()` | 길이 | 배열은 `length` 필드, String은 **메서드** |
    | `charAt(i)` | i번째 char | 범위를 벗어나면 `StringIndexOutOfBoundsException` |
    | `substring(a, b)` | 부분 문자열 | Java 7u6+ 부터 배열을 **복사**함 |
    | `indexOf` / `lastIndexOf` | 위치 | 없으면 `-1` (`0`이 아님) |
    | `contains` / `startsWith` / `endsWith` | 포함 여부 | 리터럴 비교 |
    | `isEmpty()` | 길이 0 | 공백만 있는 문자열은 false |
    | `isBlank()` | 비었거나 공백뿐 | Java 11+ |
    | `strip()` / `trim()` | 앞뒤 공백 제거 | 기준이 다름 (아래 참고) |
    | `repeat(n)` | n번 반복 | Java 11+ |
    | `lines()` | 줄 단위 Stream | Java 11+ |
    | `chars()` | 코드 유닛 IntStream | Java 9+ |

+ ⚠️ **`trim()`과 `strip()`은 기준이 다름**

    ```java
    "\u3000abc".trim();    // "\u3000abc"  ← U+0020 이하만 지우므로 전각 공백이 남음
    "\u3000abc".strip();   // "abc"         ← Character.isWhitespace 기준 (Java 11+)
    ```

    - `trim()`은 **U+0020 이하**만, `strip()`은 **`Character.isWhitespace`가 true인 것**을 지움
    - 전각 공백(U+3000)처럼 사용자 입력에 섞여 들어오는 공백은 `trim()`으로 지워지지 않음
    - ⚠️ 반대로 **NBSP(U+00A0)는 둘 다 못 지움** — `Character.isWhitespace`가 `false`이기 때문
        + 웹에서 긁어온 텍스트에 자주 섞임. 지우려면 일반 공백으로 먼저 치환해야 함

+ ⚠️ **`replace`는 리터럴, `replaceAll`은 정규식**

    ```java
    "a.b".replace(".", "-");      // "a-b"
    "a.b".replaceAll(".", "-");   // "---"  ← "."이 "모든 문자"로 해석됨
    ```

    - 이름만 보면 "하나만 vs 전부"로 오해하기 쉬운데, 둘 다 전부 바꿈. 차이는 **정규식 여부**
+ ⚠️ **`split()`의 함정**

    ```java
    "a,b,,".split(",");        // ["a", "b"]          ← 뒤쪽 빈 문자열이 사라짐
    "a,b,,".split(",", -1);    // ["a", "b", "", ""]  ← limit을 음수로 주면 유지
    "a.b.c".split(".");        // []                  ← "."은 정규식
    "a.b.c".split("\\.");      // ["a", "b", "c"]
    ```

    - CSV를 `split(",")`으로 파싱하면 마지막 빈 컬럼이 조용히 사라짐
+ ⚠️ **`substring`은 Java 7u6 이전에는 원본 배열을 공유했음**
    - 10MB 문자열에서 10자만 잘라 보관해도 10MB가 회수되지 않는 메모리 누수가 있었음
    - 지금은 복사하므로 안전하지만, 반대로 **큰 문자열을 반복해서 자르면 복사 비용이 듦**
+ ⚠️ **`String.valueOf(null)`은 컴파일은 되고 실행 시 NPE**

    ```java
    String.valueOf(null);           // ❌ char[] 오버로드가 선택됨 → NPE
    String.valueOf((Object) null);  // ✅ "null"
    ```

+ `+`로 이어붙일 때 null은 문자열 `"null"`이 됨 — 예외가 나지 않아 더 헷갈림

    ```java
    String name = null;
    System.out.println("name=" + name);   // "name=null"
    ```

### char, 코드 포인트, 그리고 length()
+ 자바의 `char`는 **문자 하나가 아니라 UTF-16 코드 유닛 하나**(16비트)
    - BMP 밖의 문자(이모지, 일부 한자)는 **서로게이트 페어 2개**로 표현됨

    ```java
    String s = "👍";
    s.length();                          // 2  ← 코드 유닛 개수
    s.codePointCount(0, s.length());     // 1  ← 실제 문자 개수
    s.charAt(0);                         // 서로게이트 조각 하나 (그 자체로는 의미 없음)
    s.substring(0, 1);                   // ❌ 깨진 문자
    ```

+ 그래서 **`length()`는 "글자 수"가 아님**
    - 글자 수 제한, 자르기, 뒤집기에서 이모지가 들어오면 깨짐
    - 코드 포인트 단위로 다루려면 `codePoints()`, `offsetByCodePoints()` 사용
+ 인코딩 변환은 항상 문자셋을 명시

    ```java
    byte[] bytes = str.getBytes(StandardCharsets.UTF_8);   // ✅
    byte[] bytes = str.getBytes();                         // ❌ 플랫폼 기본 인코딩에 의존
    ```

    - Java 18+ 부터 파일 기본 인코딩이 UTF-8로 통일됐지만(JEP 400), 명시하는 습관이 안전함

### 여러 문자열을 합칠 때
+ **`String.join()`** — 구분자로 잇기 (Java 8+)

    ```java
    String.join(", ", "a", "b", "c");   // "a, b, c"
    String.join(", ", list);            // Iterable도 받음
    ```

+ **`StringJoiner`** — 접두·접미까지 필요할 때 (Java 8+)

    ```java
    StringJoiner sj = new StringJoiner(", ", "[", "]");
    sj.setEmptyValue("[]");
    sj.add("a").add("b");              // "[a, b]"
    ```

+ **`Collectors.joining()`** — 스트림에서 바로

    ```java
    list.stream().map(User::getName).collect(Collectors.joining(", "));
    ```

+ **`String.format()`** — 서식이 필요할 때

    ```java
    String.format("%s님의 잔액은 %,d원", name, balance);
    "%s / %s".formatted(a, b);          // Java 15+
    ```

    - `Formatter`를 매번 만들고 서식 문자열을 파싱하므로 **단순 연결보다 느림**
    - ⚠️ 로케일 의존 — `%,.2f`가 어떤 환경에서는 `1.234,56`이 됨. 기계가 읽을 문자열이면 `Locale.ROOT` 지정
+ 로그는 문자열을 직접 만들지 말고 플레이스홀더에 맡기기

    ```java
    log.debug("user={} amount={}", user, amount);    // ✅ 로그 레벨이 꺼져 있으면 조립 자체를 안 함
    log.debug("user=" + user + " amount=" + amount); // ❌ 항상 조립됨
    ```

### 텍스트 블록 (Java 15+)
+ 여러 줄 문자열을 이스케이프 없이 그대로 쓸 수 있음

    ```java
    String json = """
            {
              "name": "Alice",
              "age": 25
            }
            """;
    ```

    - 공통 들여쓰기는 자동으로 제거됨 — **닫는 `"""`의 위치까지 포함해서** 가장 왼쪽을 기준으로 계산
    - 줄 끝에 `\`를 붙이면 줄바꿈 없이 이어지고, `\s`는 지워질 공백을 남김
+ SQL, JSON, HTML을 코드에 넣을 때 가독성이 크게 좋아짐
+ 값을 끼워 넣을 때는 `formatted()`와 함께 쓰는 게 보통

    ```java
    String sql = """
            SELECT * FROM member
             WHERE id = %d
            """.formatted(id);
    ```

    - ⚠️ SQL에 값을 직접 끼워 넣는 건 SQL 인젝션 통로가 됨. 실제로는 바인딩 파라미터를 쓸 것

### switch에서 String 쓰기
+ Java 7부터 `switch`에 `String`을 쓸 수 있음
+ 내부적으로는 **`hashCode()`로 후보를 좁힌 뒤 `equals()`로 확인**하는 코드로 컴파일됨
    - 해시 충돌까지 고려하므로 결과가 틀릴 일은 없음
+ ⚠️ **`switch` 대상이 `null`이면 `NullPointerException`**
    - `case null`은 Java 21의 패턴 매칭 switch에서야 가능해짐

### 언제 무엇을 쓸까
+ **`String`** — 값이 바뀌지 않는 문자열. 상수, 키, DTO 필드
+ **`StringBuilder`** — 반복문·조건문을 거치며 문자열을 조립할 때
+ **`StringBuffer`** — 인스턴스를 여러 스레드가 공유하는 게 확실할 때만. 사실상 레거시
+ **`+` 연산자** — 한 표현식 안에서 끝나는 연결. 가독성이 우선
+ **`String.join` / `Collectors.joining`** — 컬렉션을 구분자로 이을 때
+ **`String.format` / 텍스트 블록** — 서식이나 여러 줄 템플릿이 필요할 때

> 판단 기준 하나 — **"이 문자열을 몇 번 다시 만드는가?"** 한 번이면 `+`, 여러 번이면 `StringBuilder`.

### 자주 하는 착각
+ "`String`은 불변이니까 `str += "x"`는 컴파일 에러다"
    - 에러가 아님. **새 객체를 만들어 참조를 옮기는 것**이라 변수는 바뀜
+ "`+` 연산은 무조건 느리다"
    - 한 표현식 안의 `+`는 컴파일러가 최적화함. 문제가 되는 건 **반복문 안에서 누적할 때**
+ "`StringBuilder`를 쓰면 항상 빠르다"
    - 한 번 이어붙일 문자열에 쓰면 객체만 하나 더 만들고 가독성만 나빠짐
+ "같은 값이면 `==`로 비교해도 된다"
    - 리터럴끼리는 **우연히** true. `new`, 런타임 조합, 입력값이 섞이면 바로 깨짐
+ "`new String("a")`는 객체를 하나 만든다"
    - 풀의 리터럴과 힙의 복사본, **둘**을 만듦
+ "`intern()`을 쓰면 메모리가 절약된다"
    - 중복이 대량일 때만 이득. 남용하면 String Pool 조회가 느려짐
+ "`length()`는 글자 수다"
    - **UTF-16 코드 유닛 수**. 이모지 하나가 2로 세짐
+ "`trim()`이면 공백은 다 지워진다"
    - U+0020 이하만 지움. 전각 공백은 `strip()`을 써야 하고, NBSP는 둘 다 못 지움
+ "`replaceAll`은 전부 바꾸고 `replace`는 하나만 바꾼다"
    - 둘 다 전부 바꿈. 차이는 **정규식이냐 리터럴이냐**
+ "`split(".")`으로 확장자를 나눌 수 있다"
    - `.`은 정규식에서 모든 문자 → 빈 배열이 나옴. `split("\\.")`
+ "`StringBuffer`를 쓰면 멀티스레드에서 안전하다"
    - **메서드 하나 단위로만** 안전함. 여러 연산을 묶으면 여전히 직접 동기화해야 함
+ "`StringBuilder`끼리 `equals()`로 내용을 비교할 수 있다"
    - 재정의되어 있지 않아 주소 비교임. `toString().equals(...)`를 써야 함
+ "`substring`은 원본을 참조하니 메모리를 아낀다"
    - Java 7u6부터 **복사**함. 오히려 큰 문자열을 반복해서 자르면 복사 비용이 듦

### 관련 문서
+ `==`와 `equals()`의 판정 규칙, `hashCode()` 계약은 [==와 equals()의 차이](==와-equals\(\)의-차이.md) 문서 참고
+ 기본형과 참조형의 저장 방식 차이는 [기본형과 참조형 타입의 차이](../03-자바-핵심-구조/기본형과-참조형-타입의-차이.md) 문서 참고
+ `final`이 불변 객체 설계에서 하는 역할은 [final 키워드 — final과 불변 객체](../04-자바와-객체지향-프로그래밍/final-키워드.md#final과-불변-객체) 문서 참고
+ 불변 객체가 스레드 안전한 이유는 [final 키워드 — final과 스레드 안전성](../04-자바와-객체지향-프로그래밍/final-키워드.md#final과-스레드-안전성) 문서 참고
+ String Pool이 올라가는 힙 영역 구조는 [자바 가상머신의 구조와 실행 매커니즘 — 런타임 데이터 영역](../03-자바-핵심-구조/자바-가상머신의-구조와-실행-매커니즘.md#런타임-데이터-영역-runtime-data-area) 문서 참고
+ `String`이 `HashMap` 키로 적합한 이유는 [컬렉션 3대장 구조와 특징의 이해 — 컬렉션이 "같음"을 판단하는 방법](../05-컬렉션-프레임워크/컬렉션-3대장-구조와-특징의-이해.md#컬렉션이-같음을-판단하는-방법) 문서 참고
+ `StringBuffer`의 `synchronized`가 보장하는 범위는 [synchronized 키워드 — synchronized가 보장하는 것](../06-멀티-스레딩과-동시성/synchronized-키워드.md#synchronized가-보장하는-것) 문서 참고

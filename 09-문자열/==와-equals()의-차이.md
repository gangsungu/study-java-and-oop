## ==와 equals()의 차이

`==`와 `equals()`의 차이는 "주소 비교 vs 값 비교"로 한 줄 요약되지만, **실제로 버그가 나는 자리는 그 요약 바깥에 있다.**
`equals()`를 재정의해 놓고 `hashCode()`를 빼먹어 `HashMap`에서 값을 못 찾거나, `Integer`를 `==`로 비교했는데 127까지만 맞거나, `BigDecimal`이 `HashSet`과 `TreeSet`에서 다르게 동작하는 식이다.
여기서는 판정 규칙과 계약, 그리고 **`==`가 조용히 틀리는 자리**를 정리한다.

### ==와 equals()의 차이 비교
+ == (주소값 비교)
    - 기본형(primitive type)에서는 값을 비교
    - 참조형(reference type)에서는 메모리 주소(객체의 참조값)를 비교
+ equals() (내용 비교)
    - 원래 Object 클래스의 메서드로, 기본적으로 ==와 동일하게 동작
    - String이나 Wrapper Class에서 오버라이딩되어 객체의 값(내용)을 비교

```java
String s1 = new String("hello");
String s2 = new String("hello");

System.out.println(s1 == s2); // false (주소값 비교)
System.out.println(s1.equals(s2)); // true (값 비교)
```

+ 용어로 정리하면
    - `==` → **동일성(identity)** — "같은 객체인가"
    - `equals()` → **동등성(equality)** — "같은 값인가"
    - 둘은 다른 질문이고, 대부분의 코드가 묻고 싶은 건 **동등성**

### Object의 기본 equals()
+ `Object.equals()`의 구현은 이게 전부

    ```java
    public boolean equals(Object obj) {
        return (this == obj);      // 재정의하지 않으면 == 와 완전히 같음
    }
    ```

+ 그래서 **직접 만든 클래스는 재정의하지 않으면 값이 같아도 `false`**

    ```java
    class Point {
        int x, y;
        Point(int x, int y) { this.x = x; this.y = y; }
    }

    new Point(1, 2).equals(new Point(1, 2));   // ❌ false
    ```

+ 값 비교로 재정의되어 있는 대표 타입
    - `String`, 래퍼 클래스(`Integer`, `Long`, `Double` …), `BigDecimal`, `BigInteger`
    - `LocalDate` 등 날짜·시간 타입
    - `List`, `Set`, `Map` 구현체 — 원소끼리 `equals()`로 비교
    - `record` — 컴파일러가 자동 생성
    - `enum` — 상수마다 인스턴스가 하나뿐이라 재정의할 필요가 없음(`final`로 막혀 있음)
+ 재정의되어 있지 **않은** 함정 타입
    - **배열** — `Object`의 것을 그대로 씀 → `Arrays.equals()` / `Arrays.deepEquals()`
    - **`StringBuilder` / `StringBuffer`** → `toString()` 후 비교
    - **`Optional`** 은 재정의되어 있지만, 값 비교 목적으로 쓸 자리가 아님

### equals()의 규약
+ `Object.equals()`의 javadoc은 지켜야 할 다섯 가지를 명시함

    | 규약 | 내용 |
    |---|---|
    | 반사성 (reflexive) | `x.equals(x)`는 항상 `true` |
    | 대칭성 (symmetric) | `x.equals(y)`가 `true`면 `y.equals(x)`도 `true` |
    | 추이성 (transitive) | `x.equals(y)`, `y.equals(z)`가 `true`면 `x.equals(z)`도 `true` |
    | 일관성 (consistent) | 객체가 바뀌지 않으면 몇 번을 호출해도 결과가 같음 |
    | null 아님 | `x.equals(null)`은 항상 `false` (예외를 던지면 안 됨) |

+ 규약을 어기면 컴파일 에러가 아니라 **컬렉션이 조용히 오작동함**
    - `List.contains()`, `Set` 중복 판정, `Map` 조회가 전부 `equals()`에 의존하기 때문
+ **대칭성이 깨지는 전형적인 예**

    ```java
    class CaseInsensitiveString {
        private final String s;

        @Override public boolean equals(Object o) {
            if (o instanceof CaseInsensitiveString cis) return s.equalsIgnoreCase(cis.s);
            if (o instanceof String str) return s.equalsIgnoreCase(str);   // ❌ 욕심
            return false;
        }
    }

    CaseInsensitiveString cis = new CaseInsensitiveString("Polish");
    cis.equals("polish");   // true
    "polish".equals(cis);   // false  ← 대칭성 위반
    ```

    - "String과도 비교되면 편하겠지"라는 배려가 규약을 깨뜨림
    - `List.contains()`가 구현체에 따라 다른 답을 내놓는 상황이 됨
+ **추이성이 깨지는 전형적인 예 — 상속으로 필드를 추가할 때**
    - `Point`를 상속한 `ColorPoint`에서 색까지 비교하려는 순간 대칭성·추이성 중 하나가 반드시 깨짐
    - 근본적으로 **구체 클래스를 상속해 값 필드를 추가하면서 equals 규약을 지킬 방법은 없음**
    - 답은 상속 대신 **컴포지션** — `ColorPoint`가 `Point`를 필드로 가지고, `asPoint()` 같은 뷰를 제공

### equals()와 hashCode()의 계약
+ **`equals()`를 재정의하면 `hashCode()`도 반드시 함께 재정의해야 함**
+ 계약
    - `equals()`가 `true`인 두 객체는 **`hashCode()`도 반드시 같아야 함**
    - `hashCode()`가 같다고 `equals()`가 `true`일 필요는 없음 (해시 충돌은 정상)
+ 어기면 생기는 일

    ```java
    class Point {
        int x, y;
        @Override public boolean equals(Object o) { /* x, y 비교 */ }
        // hashCode() 재정의 안 함 → Object의 것(주소 기반)이 쓰임
    }

    Set<Point> set = new HashSet<>();
    set.add(new Point(1, 2));
    set.contains(new Point(1, 2));   // ❌ false
    ```

    - `HashMap`·`HashSet`은 **`hashCode()`로 버킷을 먼저 찾고** 그 안에서 `equals()`로 확인함
    - 해시가 다르면 아예 다른 버킷을 뒤지므로 `equals()`는 호출조차 되지 않음
+ ⚠️ **가변 필드를 `hashCode()`에 넣으면 컬렉션에 넣은 뒤 값을 바꾸는 순간 잃어버림**

    ```java
    Set<Member> set = new HashSet<>();
    set.add(member);
    member.setName("변경");     // 해시가 바뀜
    set.contains(member);      // ❌ false — 원래 버킷에 없음
    ```

    - 해시 기반 컬렉션의 키는 **불변**이어야 함. `String`이 키로 이상적인 이유

### 올바른 equals() 구현
+ 표준적인 형태

    ```java
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;                       // 1. 자기 자신이면 즉시 true (성능)
        if (o == null || getClass() != o.getClass()) return false;   // 2. 타입 확인
        Member other = (Member) o;                        // 3. 캐스팅
        return age == other.age                           // 4. 필드 비교
            && Objects.equals(name, other.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, age);                   // equals에서 쓴 필드와 같은 집합
    }
    ```

+ **`getClass()` vs `instanceof`**

    | | `getClass()` | `instanceof` |
    |---|---|---|
    | 하위 클래스 | 다른 타입으로 취급 | 같은 타입으로 취급 |
    | 대칭성 | 항상 지켜짐 | 하위 클래스가 필드를 추가하면 깨질 수 있음 |
    | 리스코프 치환 원칙 | 위배 소지 | 만족 |

    - 상속을 허용하지 않을 거라면 `getClass()` + 클래스를 `final`로
    - 상속 계층 전체를 같은 값으로 볼 거라면 `instanceof` + **하위에서 equals를 재정의하지 않기**
    - 애초에 값 클래스는 `final`이나 `record`로 만드는 게 가장 깔끔함
+ **필드 타입별 비교 방법**

    | 타입 | 비교 |
    |---|---|
    | 기본형 (float·double 제외) | `==` |
    | `float` | `Float.compare(a, b) == 0` |
    | `double` | `Double.compare(a, b) == 0` |
    | 참조형 | `Objects.equals(a, b)` (null 안전) |
    | 배열 | `Arrays.equals()` / 중첩이면 `Arrays.deepEquals()` |

    - `float`/`double`을 `==`로 비교하면 `NaN`과 `-0.0`에서 어긋남 (아래 참고)
+ **비교 순서가 성능을 좌우함** — 다를 가능성이 높고 계산이 싼 필드를 먼저
+ ⚠️ **시그니처를 틀리면 재정의가 아니라 오버로딩이 됨**

    ```java
    public boolean equals(Member m) { ... }   // ❌ Object가 아니라 Member를 받음
    ```

    - 컴파일은 되지만 컬렉션은 `Object`를 받는 쪽을 호출하므로 전혀 동작하지 않음
    - **`@Override`를 붙이면 컴파일 에러로 잡힘** — 반드시 붙일 것
+ **`record`는 이 전부를 자동으로 만들어 줌** (Java 16+)

    ```java
    record Member(String name, int age) { }   // equals, hashCode, toString 자동 생성
    ```

    - 모든 컴포넌트를 비교 대상으로 삼고, 규약도 지켜짐
    - 값 객체(DTO, VO)라면 `record`가 사실상 정답

### null 안전하게 비교하기
+ `Objects.equals(a, b)` — 양쪽 다 null일 수 있을 때

    ```java
    Objects.equals(null, null);    // true
    Objects.equals(null, "a");     // false
    a.equals(b);                   // ❌ a가 null이면 NPE
    ```

+ 리터럴이나 상수를 **앞에** 두는 관용구

    ```java
    if ("ADMIN".equals(role)) { ... }        // ✅ role이 null이어도 안전
    if (role.equals("ADMIN")) { ... }        // ❌ role이 null이면 NPE
    ```

    - 읽는 순서가 어색해서 호불호가 있지만, 널 체크 한 줄을 줄여 줌
    - 팀 규칙이 "널을 애초에 안 넘긴다"라면 굳이 쓰지 않아도 됨
+ `enum`은 `==`가 null 안전

    ```java
    if (status == Status.ACTIVE) { ... }     // status가 null이어도 그냥 false
    ```

### ==가 조용히 틀리는 자리
+ **문자열**

    ```java
    String a = "hello";
    String b = "hel" + "lo";                 // 상수 폴딩 → 같은 객체
    String c = new String("hello");
    String d = readFromInput();              // 런타임에 만들어진 값

    a == b;   // true   ← 우연히 통과
    a == c;   // false
    a == d;   // false  ← 테스트는 통과했는데 운영에서 터지는 패턴
    ```

    - 자세한 내용은 [String 클래스 비교 — String Pool과 리터럴](String-클래스-비교.md#string-pool과-리터럴) 참고
+ **래퍼 클래스의 캐싱** — 가장 악명 높은 함정

    ```java
    Integer a = 127, b = 127;
    System.out.println(a == b);      // true   ← 캐시된 같은 객체

    Integer c = 128, d = 128;
    System.out.println(c == d);      // false  ← 캐시 범위 밖이라 새 객체
    System.out.println(c.equals(d)); // true
    ```

    - 오토박싱은 `Integer.valueOf()`를 호출하고, 이 메서드가 **-128 ~ 127을 미리 만들어 캐싱**함
    - 캐시 범위: `Byte`·`Short`·`Integer`·`Long`은 -128~127, `Character`는 0~127, `Boolean`은 두 값 전부
        + `Integer`의 상한만 `-XX:AutoBoxCacheMax`로 조정 가능
    - `Float`·`Double`은 캐시가 없어 항상 새 객체
    - **작은 값으로 테스트하면 통과하고 큰 값에서 깨지는** 최악의 형태
+ **언박싱이 섞이면 오히려 `==`가 맞음**

    ```java
    Integer boxed = 1000;
    int primitive = 1000;
    boxed == primitive;              // true — 한쪽이 기본형이면 언박싱 후 값 비교
    ```

    - 규칙이 헷갈리므로, **래퍼끼리는 언제나 `equals()`나 `intValue()` 비교**
    - ⚠️ 이때 `boxed`가 null이면 언박싱하다 NPE
+ **부동소수점**

    ```java
    0.1 + 0.2 == 0.3;                                  // false — 근사값 오차
    Double.NaN == Double.NaN;                          // false — NaN은 자기 자신과도 다름
    Double.valueOf(Double.NaN).equals(Double.NaN);     // ✅ true — equals는 NaN을 같다고 봄
    0.0 == -0.0;                                       // true
    Double.valueOf(0.0).equals(-0.0);                  // ❌ false — equals는 다르다고 봄
    ```

    - `==`와 `equals()`의 결과가 **정반대로 갈리는** 유일한 기본 타입
    - `Double.compare()`가 `equals()`와 같은 기준(총 순서)을 씀
    - 금액 계산은 애초에 `BigDecimal`
+ **배열**

    ```java
    int[] a = {1, 2}, b = {1, 2};
    a == b;                    // false
    a.equals(b);               // false ← Object의 equals라 == 와 같음
    Arrays.equals(a, b);       // ✅ true
    Arrays.deepEquals(m1, m2); // 2차원 이상
    ```

### equals()와 compareTo()의 불일치
+ `Comparable`의 javadoc은 **`compareTo()` 결과가 `equals()`와 일관될 것을 강력히 권고**함 (강제는 아님)
+ 대표적인 위반 사례가 `BigDecimal`

    ```java
    BigDecimal a = new BigDecimal("1.0");
    BigDecimal b = new BigDecimal("1.00");

    a.equals(b);        // false ← scale(소수 자릿수)까지 비교
    a.compareTo(b);     // 0     ← 값만 비교
    ```

+ 그래서 컬렉션에 따라 결과가 달라짐

    ```java
    Set<BigDecimal> hashSet = new HashSet<>();     // equals/hashCode 기준
    hashSet.add(a); hashSet.add(b);                // 크기 2

    Set<BigDecimal> treeSet = new TreeSet<>();     // compareTo 기준
    treeSet.add(a); treeSet.add(b);                // 크기 1
    ```

    - **`TreeSet`·`TreeMap`은 `equals()`를 아예 쓰지 않고 `compareTo()`/`Comparator`로만 판단함**
    - 금액을 값으로 비교하려면 `compareTo(other) == 0`을 쓸 것

### ==를 써야 하는 자리
+ **기본형 비교** — `int`, `long`, `char`, `boolean`
+ **enum 비교** — 인스턴스가 유일하므로 `==`가 정확하고, null 안전하며, 오타가 컴파일 에러로 잡힘
+ **null 검사** — `if (obj == null)`
+ **동일성 자체가 목적일 때** — 캐시에서 꺼낸 게 원본과 같은 객체인지 확인하는 등
+ 그 외 참조형은 전부 `equals()`

### 실무 기준
+ **값 비교는 `equals()`, 동일성 확인만 `==`**
+ **`equals()`를 재정의하면 `hashCode()`도 함께 재정의한다**
    - IDE 생성 기능이나 Lombok `@EqualsAndHashCode`를 쓰더라도 **어떤 필드가 들어갔는지는 직접 확인**
+ **`@Override`를 반드시 붙인다** — 시그니처 실수를 컴파일 타임에 잡아 줌
+ **해시 기반 컬렉션의 키는 불변으로 만든다**
+ **값 객체는 `record`나 `final` 클래스로** — 상속이 끼면 규약을 지키기 어려움
+ **래퍼 클래스끼리 `==`로 비교하지 않는다**
+ **JPA 엔티티는 `equals`/`hashCode`를 함부로 만들지 않는다**
    - ID가 아직 없는 상태에서 컬렉션에 넣으면 저장 후 찾지 못함
    - 필요하다면 비즈니스 키 기반으로 만들고, `hashCode()`는 상수로 두는 절충안도 씀

### 자주 하는 착각
+ "`equals()`는 항상 값을 비교한다"
    - 재정의한 클래스만. `Object`의 기본 구현은 `==`와 같음
+ "같은 값이면 문자열도 `==`로 비교할 수 있다"
    - 리터럴끼리는 **우연히** 맞을 뿐. 런타임에 만들어진 문자열은 어긋남
+ "`Integer`는 값 타입이니 `==`로 비교해도 된다"
    - -128~127만 캐시라 128부터 깨짐
+ "`hashCode()`가 같으면 같은 객체다"
    - 해시 충돌은 정상. 최종 판정은 `equals()`
+ "`equals()`만 재정의하면 `HashMap`에서 찾을 수 있다"
    - 버킷을 못 찾아 `equals()`가 호출조차 되지 않음
+ "`hashCode()`를 안 만들어도 `List`는 잘 동작하니 괜찮다"
    - `ArrayList.contains()`는 `equals()`만 쓰지만, 언젠가 `Set`이나 `Map`에 담기는 순간 터짐
+ "`equals(Member m)`로 만들어도 재정의된다"
    - 오버로딩일 뿐. 컬렉션은 `equals(Object)`를 호출함
+ "배열도 `equals()`로 내용 비교가 된다"
    - `Object`의 것이라 주소 비교. `Arrays.equals()`를 써야 함
+ "`double`은 `==`로 비교하면 된다"
    - `NaN`은 자기 자신과도 다르고, `-0.0`은 `equals()`와 결과가 반대
+ "`BigDecimal`은 값이 같으면 `equals()`도 true다"
    - `1.0`과 `1.00`은 `equals()`로 다름. 값 비교는 `compareTo() == 0`
+ "`TreeSet`도 `equals()`로 중복을 판단한다"
    - `compareTo()`/`Comparator`만 봄. `equals()`는 호출되지 않음
+ "`enum`은 `equals()`로 비교하는 게 안전하다"
    - `==`가 더 안전함. null이어도 예외가 나지 않고 타입 오류를 컴파일 타임에 잡아 줌

### 관련 문서
+ String Pool과 리터럴 때문에 `==`가 우연히 통과하는 이유는 [String 클래스 비교 — String Pool과 리터럴](String-클래스-비교.md#string-pool과-리터럴) 문서 참고
+ 기본형과 참조형이 값을 저장하는 방식 차이는 [기본형과 참조형 타입의 차이](../03-자바-핵심-구조/기본형과-참조형-타입의-차이.md) 문서 참고
+ 해시 기반 컬렉션이 `hashCode()`와 `equals()`를 쓰는 순서는 [컬렉션 3대장 구조와 특징의 이해 — 컬렉션이 "같음"을 판단하는 방법](../05-컬렉션-프레임워크/컬렉션-3대장-구조와-특징의-이해.md#컬렉션이-같음을-판단하는-방법) 문서 참고
+ `TreeSet`이 `Comparator`로 중복을 판정하는 동작은 [다양한 set의 구현체 비교 — TreeSet](../05-컬렉션-프레임워크/다양한-set의-구현체-비교.md#treeset) 문서 참고
+ 오버라이딩과 오버로딩의 구분은 [자바의 OOP — 오버라이딩](../04-자바와-객체지향-프로그래밍/자바의-OOP.md#오버라이딩) 문서 참고
+ 상속 대신 컴포지션을 택하는 기준은 [객체지향 설계원칙](../02-객체지향-설계원칙/객체지향-설계원칙.md) 문서 참고
+ 불변 객체가 해시 키로 안전한 이유는 [final 키워드 — final과 불변 객체](../04-자바와-객체지향-프로그래밍/final-키워드.md#final과-불변-객체) 문서 참고
+ `enum` 인스턴스가 유일하게 유지되는 원리는 [직렬화와 역직렬화 — 싱글턴과 역직렬화](../08-객체-직렬화/직렬화와-역직렬화.md#싱글턴과-역직렬화) 문서 참고

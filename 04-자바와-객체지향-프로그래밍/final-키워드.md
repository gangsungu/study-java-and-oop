## final 키워드

`final`은 **한 번 정해진 것을 다시 바꾸지 못하게 잠그는 제어자**다.
붙는 자리에 따라 잠기는 대상이 달라지고, 특히 변수에 붙었을 때 **"값이 불변"이 아니라 "재할당 금지"** 라는 점에서 오해가 가장 많이 생긴다.

### final 키워드
+ 변수, 메서드, 클래스에 사용되어 변경을 제한하는 역할

    | 붙는 곳 | 막는 것 | 한 문장 |
    |---------|---------|---------|
    | 변수 | 재할당 | **다른 값을 다시 대입할 수 없음** (상수 역할) |
    | 메서드 | 오버라이딩 | 자식이 동작을 **바꿔치기할 수 없음** |
    | 클래스 | 상속 | 자식 클래스를 **만들 수 없음** |

    ```java
    final class Animal {} // 상속 불가
    class Dog extends Animal {} // 오류 발생

    class Parent {
        final void show() {
            System.out.println("Cannot be overridden");
        }
    }

    class Child extends Parent {
        void show() {
            // 오류 발생 (final 메서드는 오버라이딩 불가)
            System.out.println("Override attempt");
        }
    }
    ```

+ 세 경우를 관통하는 한 줄
    - **`final`은 "그 이름이 가리키는 대상"을 고정할 뿐, "그 대상의 내부"까지 고정하지 않는다**
    - 이 문서에 나오는 거의 모든 함정이 이 한 줄에서 갈린다

+ `final`은 접근 제어자가 아님
    - `public`·`protected`·`private`과 같은 줄에 쓰지만 역할이 다름 → [자바의 접근 제어자](../03-자바-핵심-구조/자바의-접근-제어자.md) 참고
    - `static`·`abstract`와 함께 **기타 제어자(modifier)** 로 묶임

### final 변수
+ 붙일 수 있는 자리

    | 자리 | 예시 | 의미 |
    |------|------|------|
    | 지역 변수 | `final int x = 10;` | 메서드 안에서 재대입 금지 |
    | 매개변수 | `void f(final int x)` | 넘어온 값을 메서드 안에서 못 바꿈 |
    | 인스턴스 필드 | `private final String name;` | 객체마다 한 번 정해지면 끝 |
    | `static` 필드 | `static final int MAX = 100;` | 클래스 전체가 공유하는 상수 |
    | `catch` 파라미터 | `catch (final IOException e)` | 예외 변수 재대입 금지 |
    | 향상된 for 변수 | `for (final String s : list)` | 반복 변수 재대입 금지 |

+ 초기화 시점 — **선언할 때 꼭 초기화해야 하는 건 아님**
    - 값 없이 선언만 해둔 `final` 필드를 **blank final**이라고 부름
    - 대신 **객체가 완성되기 전까지 정확히 한 번** 값이 채워져야 함

    | 종류 | 초기화 가능한 위치 |
    |------|-------------------|
    | 인스턴스 `final` 필드 | 선언 시 · 인스턴스 초기화 블록 · **모든 생성자** |
    | `static final` 필드 | 선언 시 · `static` 초기화 블록 |
    | 지역 `final` 변수 | 선언 시 · 처음 쓰기 전 어디서든 (한 번만) |

    ```java
    class Member {
        private final String id;      // blank final — 여기서 초기화하지 않음
        private final long createdAt;

        Member(String id) {
            this.id = id;             // 생성자에서 채움 → OK
            this.createdAt = System.currentTimeMillis();
        }

        Member() {
            // this.id를 채우지 않으면 컴파일 에러
            // "variable id might not have been initialized"
            this("anonymous");        // 다른 생성자에 위임해도 됨
        }
    }
    ```

    - 생성자마다 값이 달라도 되지만, **어느 경로로 들어와도 반드시 채워져야 함**
    - `this(...)`로 위임한 뒤 다시 대입하면 **두 번 대입이 되어 컴파일 에러**
    - 그래서 `final` 필드는 "생성자에서 값이 정해지고 그 뒤로는 절대 안 바뀐다"는 보장을 준다

+ 참조형에 붙였을 때 — **가장 자주 걸리는 지점**
    - `final`이 잠그는 것은 **변수가 들고 있는 참조(주소)** 이지 객체의 내부 상태가 아님

    ```java
    final List<String> list = new ArrayList<>();
    list.add("a");              // ⭕ 내부 상태 변경은 자유
    list.clear();               // ⭕
    list = new ArrayList<>();   // ❌ 컴파일 에러 — 다른 객체를 가리킬 수 없음

    final int[] arr = {1, 2, 3};
    arr[0] = 99;                // ⭕ 배열 원소도 그대로 바뀜
    ```

    - 기본형과 참조형에서 `final`의 의미가 갈리는 이유는 [기본형과 참조형 타입의 차이](../03-자바-핵심-구조/기본형과-참조형-타입의-차이.md) 참고
    - 내용까지 막고 싶으면 **불변 컬렉션**을 써야 함

    ```java
    private final List<String> a = List.of("x", "y");                    // 변경 시 UnsupportedOperationException
    private final List<String> b = Collections.unmodifiableList(origin); // 원본이 바뀌면 같이 바뀜(뷰)
    private final List<String> c = List.copyOf(origin);                  // 복사본이라 원본과 무관
    ```

    - `unmodifiableList`는 **원본을 감싼 뷰**라서 원본을 바꾸면 그대로 비침 → 진짜 고정하려면 `List.copyOf()`

+ 매개변수에 붙이는 `final`
    - 메서드 안에서 매개변수를 다른 값으로 덮어쓰는 실수를 막아줌
    - 다만 **호출한 쪽에는 아무 영향이 없음** — 자바는 항상 값을 복사해 전달하므로 애초에 호출자의 변수는 바뀌지 않음

    ```java
    void f(final StringBuilder sb) {
        sb.append("!");              // ⭕ 객체 내부는 여전히 바꿀 수 있음
        // sb = new StringBuilder(); // ❌ 재대입만 막힘
    }
    ```

    - 즉 `final` 매개변수는 **호출자를 위한 보호가 아니라 메서드 본문을 읽는 사람을 위한 표시**

### 컴파일 타임 상수
+ `final` 변수 중 아래 조건을 **모두** 만족하면 자바는 이를 상수(constant variable)로 취급하고 **사용처에 값을 그대로 박아 넣음**
    - 타입이 **기본형 또는 `String`**
    - **컴파일 시점에 값이 확정되는 식**으로 초기화됨 (리터럴, 리터럴끼리의 연산 등)

    ```java
    static final int    MAX  = 100;           // ⭕ 상수 — 사용처에 100이 박힘
    static final String NAME = "ja" + "va";   // ⭕ 상수 — "java"로 합쳐져 박힘
    static final int    SIZE = compute();     // ❌ 실행해봐야 아는 값 → 상수 아님
    static final List<String> L = List.of();  // ❌ 참조형 → 상수 아님
    ```

+ 그래서 생기는 효과
    - `switch`의 `case` 라벨, 어노테이션 인자처럼 **컴파일 시점 값이 필요한 자리에 쓸 수 있음**
    - 상수만 읽는 접근은 **클래스 초기화를 일으키지 않음** → [static 키워드 — static final 상수](../03-자바-핵심-구조/static-키워드.md#static-final-상수) 참고

+ 그래서 생기는 함정 — **상수 값을 고쳐도 쓰는 쪽을 다시 컴파일하지 않으면 옛날 값이 남음**

    ```java
    // A.java
    public class A { public static final int VERSION = 1; }

    // B.java
    System.out.println(A.VERSION);   // 컴파일되면 이 자리에 1이 박힘
    ```

    - `A`만 `VERSION = 2`로 고쳐 다시 컴파일해도 `B.class`에는 여전히 `1`이 남아 있음
    - 라이브러리를 나눠 배포할 때 실제로 겪는 문제 → 바뀔 수 있는 값은 상수 대신 **메서드로 노출**하는 편이 안전

### final 메서드
+ 자식이 **오버라이딩할 수 없게** 잠금
    - `static` 메서드에 붙이면 **하이딩도 막힘**
+ 쓰는 이유
    - **동작이 바뀌지 않는다는 보장**이 필요할 때 — 특히 부모가 흐름을 고정하고 일부만 자식에게 위임하는 템플릿 메서드 패턴

    ```java
    abstract class Report {
        public final void print() {   // 전체 흐름은 자식이 못 건드림
            header();
            body();                   // 달라지는 부분만 위임
            footer();
        }
        protected abstract void body();
    }
    ```

    - **생성자에서 호출하는 메서드**를 잠글 때 — 생성자 안에서 오버라이딩 가능한 메서드를 부르면 자식 필드가 초기화되기 전에 자식 구현이 실행됨 → [자바의 OOP — 생성자](자바의-OOP.md#생성자) 참고

+ 굳이 붙일 필요 없는 경우
    - `private` 메서드 — 자식에게 보이지 않아 애초에 오버라이딩 대상이 아님
    - `final` 클래스 안의 메서드 — 상속 자체가 불가능하므로 이미 잠긴 것과 같음

### final 클래스
+ 상속을 금지함 → 자식 클래스가 존재할 수 없음
+ 표준 라이브러리의 대표적인 `final` 클래스
    - `String`, `Integer`·`Long` 등 래퍼 클래스, `LocalDate`·`LocalDateTime`, `System`, `Math`
+ `String`이 `final`인 이유 (면접 단골)
    - **불변 보장** — 상속해서 내용을 바꾸는 자식이 생기면 불변이라는 전제가 무너짐
    - **문자열 풀** — 같은 리터럴을 여러 곳이 공유하는데 내용이 바뀌면 공유하는 모든 곳이 함께 오염됨
    - **해시 캐싱** — `hashCode()`를 한 번 계산해 캐시할 수 있는 것도 값이 안 바뀌기 때문
    - **보안** — 파일 경로·클래스 이름 검증을 통과한 뒤 값이 바뀌는 공격을 원천 차단
+ 암묵적으로 `final`이라 직접 붙일 필요가 없는 것들
    - `record` (Java 16+)
    - `enum` (상수별 본문이 없는 경우)
    - 익명 클래스

### final을 붙일 수 없는 곳
| 자리 | 이유 |
|------|------|
| 생성자 | 애초에 상속·오버라이딩 대상이 아님 |
| `abstract` 메서드 | "자식이 반드시 구현하라"와 "자식이 못 건드린다"가 모순 |
| `interface`의 메서드 (`default` 포함) | 구현 클래스가 재정의할 수 있어야 하므로 `final` 불가 |
| `interface` 자체 | 인터페이스는 암묵적 `abstract` |

+ 반대로 **인터페이스의 필드는 쓰지 않아도 이미 `public static final`**

    ```java
    interface Config {
        int MAX = 100;                    // = public static final int MAX = 100;
        public static final int MIN = 0;  // 똑같지만 중복이라 보통 생략
    }
    ```

### effectively final (Java 8+)
+ `final`을 붙이진 않았지만 **한 번 값이 정해진 뒤 재대입이 없는** 지역 변수
+ 람다·익명 클래스·지역 클래스가 지역 변수를 캡처하려면 **`final`이거나 effectively final이어야 함**

    ```java
    int count = 0;
    Runnable r = () -> System.out.println(count);   // ⭕ 재대입이 없으므로 effectively final
    // count = 1;                                   // ❌ 이 줄을 추가하면 위 람다가 컴파일 에러
    ```

+ 왜 이런 제약이 있나 — **스레드 때문이 아니라 지역 변수가 스택에 있기 때문**
    - 지역 변수는 메서드의 스택 프레임에 있고, 메서드가 끝나면 사라짐
    - 람다는 그 변수보다 오래 살 수 있으므로 **값을 복사해서 들고 감**
    - 그러면 원본과 복사본이 따로 놀게 되는데, 어느 쪽이 진짜인지 헷갈리는 코드가 되므로 자바는 **아예 변경을 금지**하는 쪽을 택함

+ **인스턴스 필드·`static` 필드는 이 제약이 없음**

    ```java
    class Counter {
        int count = 0;                     // 필드는 힙에 있음
        Runnable r = () -> count++;        // ⭕ 자유롭게 변경 가능
    }
    ```

    - 필드는 힙에 있고 람다는 `this` 참조를 통해 접근하므로 복사본 문제가 생기지 않음
    - 잘 알려진 회피 패턴이 이 원리를 이용한 것

    ```java
    int[] box = {0};                       // 배열(객체)은 힙에 있음
    list.forEach(s -> box[0]++);           // 컴파일은 통과함
    ```

    - 배열 트릭은 **컴파일러를 통과시킬 뿐** — 의도가 드러나지 않고 병렬 스트림에서는 그대로 깨짐
    - 누적이 필요하면 `AtomicInteger`(스레드 안전)나 스트림의 `reduce`·`collect`를 쓰는 게 맞음

+ 반복문 변수의 차이

    ```java
    for (int i = 0; i < 3; i++) {
        list.add(() -> System.out.println(i));   // ❌ i는 매 반복 재대입됨
    }

    for (String s : items) {
        list.add(() -> System.out.println(s));   // ⭕ s는 반복마다 새로 만들어짐
    }
    ```

+ `final`을 명시하는 것과의 차이
    - 캡처 가능 여부에서는 **완전히 같음**
    - 다만 `final`을 붙여두면 실수로 재대입했을 때 **그 줄에서 바로** 에러가 나고, 안 붙이면 **한참 떨어진 람다 쪽에서** 에러가 남

### final과 불변 객체
+ `final`은 불변 객체를 만드는 **재료일 뿐 그 자체가 불변을 보장하지 않음**
+ 불변 클래스가 되려면
    - ① 클래스를 `final`로 (상속으로 규칙을 깨지 못하게)
    - ② 모든 필드를 `private final`로
    - ③ setter를 두지 않음
    - ④ 가변 객체를 필드로 받으면 **생성자에서 방어적 복사**
    - ⑤ 가변 객체를 반환할 때도 **복사본을 반환**

    ```java
    final class Period {
        private final Date start;
        private final Date end;

        Period(Date start, Date end) {
            this.start = new Date(start.getTime());   // ④ 방어적 복사
            this.end   = new Date(end.getTime());     //    안 하면 호출자가 나중에 바꿔버릴 수 있음
        }

        Date getStart() {
            return new Date(start.getTime());         // ⑤ 내부 객체를 그대로 주면 밖에서 바뀜
        }
    }
    ```

    - ④·⑤를 빼먹으면 필드가 `final`이어도 **객체 상태가 밖에서 바뀜**

    ```java
    Date d = new Date();
    Period p = new Period(d, d);
    d.setTime(0);          // 방어적 복사가 없으면 p의 내부까지 함께 바뀜
    ```

    - 애초에 `LocalDate`처럼 **불변 타입을 쓰면 복사 자체가 필요 없음**
+ `record`는 이 중 ①②③을 문법으로 강제해줌 — 다만 ④⑤(가변 필드 방어)는 여전히 직접 해야 함

### final과 스레드 안전성
+ `final` 필드는 **안전 발행(safe publication)** 을 보장받음
    - 생성자가 정상적으로 끝나면, 다른 스레드는 별도 동기화 없이도 **`final` 필드의 올바른 값을 봄**
    - JVM이 생성자 끝에 메모리 배리어를 넣어 "필드 쓰기"가 "객체 참조 공개"보다 먼저 보이도록 보장하기 때문
+ 일반 필드에는 이 보장이 없음
    - 객체 참조는 이미 보이는데 필드는 아직 기본값(`null`/`0`)으로 보이는 상황이 실제로 발생할 수 있음
+ 다만 **생성자 안에서 `this`가 밖으로 새면 보장이 깨짐**

    ```java
    class Broken {
        final int value;
        Broken() {
            Registry.register(this);   // ❌ 아직 완성되지 않은 객체가 공개됨
            this.value = 42;
        }
    }
    ```

+ 그래서 멀티스레드 환경에서 공유하는 객체는 **불변 + `final` 필드**로 만드는 것이 가장 단순한 해법
    - 상태가 바뀌지 않으면 동기화 자체가 필요 없음
    - `static` 가변 상태의 위험은 [static 키워드 — 주의점](../03-자바-핵심-구조/static-키워드.md#주의점) 참고

### final과 성능
+ **성능을 노리고 붙일 이유는 없음**
    - "`final` 메서드는 인라인되어 빠르다"는 건 옛날 이야기
    - JIT는 실제로 로딩된 클래스를 관찰해서 구현이 하나뿐이면 알아서 인라인함 → [자바 가상머신의 구조와 실행 매커니즘](../03-자바-핵심-구조/자바-가상머신의-구조와-실행-매커니즘.md) 참고
    - 나중에 자식 클래스가 로딩되면 되돌리는 역최적화까지 갖춰져 있음
+ 실제로 바이트코드가 달라지는 건 **컴파일 타임 상수 인라인** 정도
+ `final`은 **성능 도구가 아니라 설계 의도를 못박는 도구**로 보는 게 맞음

### final과 sealed (Java 17+)
+ `final`은 상속을 **완전히** 막고, `sealed`는 **허락한 클래스만** 상속을 허용함

    ```java
    sealed interface Shape permits Circle, Square {}

    final class Circle implements Shape {}      // 더 이상 확장 불가
    non-sealed class Square implements Shape {} // 여기서부터는 자유롭게 확장 가능
    ```

    - `sealed` 타입을 상속·구현하는 클래스는 반드시 `final`·`sealed`·`non-sealed` 중 하나여야 함
+ 세 단계로 정리하면
    - `final` — 닫음
    - `sealed` — 정해둔 만큼만 엶
    - 아무것도 안 붙임 — 완전히 엶
+ 하위 타입이 확정되므로 `switch` 패턴 매칭에서 **모든 경우를 다뤘는지 컴파일러가 검사**해줌 → [다형성 — 패턴 매칭](다형성.md#업캐스팅과-다운캐스팅) 참고

### final · finally · finalize
+ 이름만 비슷하고 서로 아무 관계가 없음

    | 이름 | 정체 | 하는 일 |
    |------|------|---------|
    | `final` | 제어자(키워드) | 변수·메서드·클래스의 변경을 막음 |
    | `finally` | 예외 처리 블록 | 예외 발생 여부와 무관하게 **항상 실행** |
    | `finalize()` | `Object`의 메서드 | GC 직전에 호출되던 메서드 — **Java 9부터 deprecated, 18부터 사실상 제거** |

    - `finalize()`는 호출 시점이 보장되지 않아 자원 해제에 쓸 수 없음 → `try-with-resources`를 씀
    - 자세한 내용은 [가비지 컬렉터 - 실무](../03-자바-핵심-구조/가비지-컬렉터-실무.md) 참고

### 실무 기준
+ **필드는 일단 `final`로 두고, 바꿔야 할 이유가 생기면 그때 푼다**
    - 생성자에서 값이 정해지고 끝나는 필드가 대부분이므로 기본값으로 삼기 좋음
    - 의존성 주입 필드(`private final Repository repo;`)가 대표적인 예 → [객체지향 설계원칙](../02-객체지향-설계원칙/객체지향-설계원칙.md) 참고
+ **클래스는 확장을 염두에 두고 설계했을 때만 연다**
    - 상속을 고려하지 않은 클래스가 상속되면 부모의 내부 구현 변경이 자식을 조용히 망가뜨림
    - 열어둘 거라면 어떤 메서드를 어떻게 오버라이딩해야 하는지 문서로 남기고, 아니면 `final`로 막거나 합성(composition)을 쓸 것
+ **지역 변수·매개변수의 `final`은 팀 컨벤션 문제**
    - 안전하지만 코드가 길어져서 붙이지 않는 팀도 많음
    - 짧은 메서드 + effectively final로 충분하다고 보는 쪽이 요즘은 더 흔함
+ **상수는 `static final` + 대문자 스네이크**
    - 참조형 상수는 내용까지 고정되지 않는다는 점을 항상 같이 기억할 것

### 자주 하는 착각
+ "`final`을 붙이면 객체가 불변이 된다"
    - 참조만 고정됨. `final List`에 `add()`는 그대로 됨. 불변은 클래스를 그렇게 설계해야 얻어짐
+ "`final` 변수는 선언할 때 반드시 초기화해야 한다"
    - blank final로 두고 **생성자나 초기화 블록에서** 채워도 됨. 단 모든 경로에서 정확히 한 번
+ "`final` 메서드가 더 빠르다"
    - JIT가 알아서 인라인하므로 실측할 만한 차이가 아님. 성능이 아니라 설계 의도로 붙일 것
+ "`private` 메서드에 `final`을 붙이면 더 안전하다"
    - 자식에게 보이지 않아 이미 오버라이딩 대상이 아님. 의미 없는 중복
+ "람다에서 지역 변수를 못 바꾸는 건 스레드 안전성 때문이다"
    - 근본 원인은 **지역 변수가 스택에 있어 람다가 값을 복사해 가기 때문**. 스레드는 그 위에 얹힌 이유일 뿐
+ "effectively final은 `final`을 붙인 것과 다른 무언가다"
    - 캡처 관점에서는 완전히 동일함. 차이는 재대입 실수를 **어디서** 잡아주느냐뿐
+ "`static final`은 항상 값이 인라인된다"
    - 기본형·`String`이면서 상수 표현식으로 초기화됐을 때만. `static final Map`은 인라인 대상이 아님
+ "인터페이스 상수에는 `public static final`을 꼭 써야 한다"
    - 안 써도 자동으로 붙음. 명시하면 중복일 뿐
+ "`enum`이나 `record`에 `final`을 붙여야 상속을 막을 수 있다"
    - 이미 암묵적으로 `final`임. 오히려 붙이면 컴파일 에러
+ "`final` 필드는 리플렉션으로도 절대 못 바꾼다"
    - `final`은 언어·JVM 수준의 규칙이지 보안 경계가 아님. 리플렉션으로 우회할 여지가 있고 제약은 자바 버전마다 다름
+ "`final`과 `finally`는 관련이 있다"
    - 이름만 비슷할 뿐 완전히 다른 것. `finalize()`까지 셋 다 무관

### 관련 문서
+ 오버라이딩 성립 조건과 `final` 메서드가 막히는 위치는 [자바의 OOP](자바의-OOP.md#오버라이딩) 문서 참고
+ 확장 지점을 여는 선택과 막는 선택의 트레이드오프는 [다형성](다형성.md#다형성을-쓰는-이유) 문서 참고
+ 기본형·참조형에서 `final`의 의미가 갈리는 이유는 [기본형과 참조형 타입의 차이](../03-자바-핵심-구조/기본형과-참조형-타입의-차이.md) 문서 참고
+ `static final` 상수와 클래스 초기화의 관계는 [static 키워드](../03-자바-핵심-구조/static-키워드.md#static-final-상수) 문서 참고
+ `final`이 접근 제어자가 아닌 이유와 제어자 조합은 [자바의 접근 제어자](../03-자바-핵심-구조/자바의-접근-제어자.md) 문서 참고
+ JIT의 인라인·역최적화는 [자바 가상머신의 구조와 실행 매커니즘](../03-자바-핵심-구조/자바-가상머신의-구조와-실행-매커니즘.md) 문서 참고
+ `finalize()`와 자원 해제는 [가비지 컬렉터 - 실무](../03-자바-핵심-구조/가비지-컬렉터-실무.md) 문서 참고

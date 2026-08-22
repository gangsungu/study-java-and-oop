## static 키워드

### static
+ 멤버를 인스턴스가 아니라 클래스에 소속시키는 제어자
    - 접근 제어자가 아님. 접근 범위가 아니라 소속을 정함
    - 접근 제어자와 함께 씀 (public static, private static ...)
+ 객체마다 따로 만들지 않고 클래스 단위로 하나만 두고 공유함
+ 붙일 수 있는 곳
    - 필드, 메서드, 초기화 블록, 중첩 클래스, import
    - 최상위 클래스와 지역 변수에는 붙일 수 없음

### 인스턴스 소속과 클래스 소속
+ 인스턴스는 메모리 영역의 이름이 아니라 new로 만들어낸 객체 하나를 뜻함
    - 힙과 스택은 "어디에 저장되는가"이고, 인스턴스는 "무엇인가"에 대한 말
    - 인스턴스는 항상 힙에 만들어짐. 스택에 있는 것은 그 인스턴스를 가리키는 참조 변수임

    ```java
    class Counter {
        static int count = 0;   // 클래스에 하나
        int id;                 // 인스턴스마다 하나
    }

    Counter c1 = new Counter();
    Counter c2 = new Counter();
    ```

    ```
    스택 (main 프레임)         힙
    ─────────────────      ──────────────────────────────
     c1  ●──────────────▶   Counter 인스턴스 #1
                             └ id = 1        ← 인스턴스 변수
     c2  ●──────────────▶   Counter 인스턴스 #2
                             └ id = 2        ← 각자 따로 가짐

                            Class<Counter> 객체 (클래스당 1개)
                             └ count = 2     ← static 변수
    ```

+ 소속에 따른 차이

    |구분|어디에 들어있나|값이 몇 개|
    |---|---|---|
    |인스턴스 변수 (id)|힙의 각 객체 몸통 안|객체 수만큼|
    |static 변수 (count)|객체 몸통 밖, 클래스 쪽|클래스당 1개|
    |지역 변수 (c1)|스택 프레임|메서드 호출마다|

+ static이 클래스에 소속된다는 것은 개별 객체 안이 아니라 그 바깥에 하나만 둔다는 뜻
    - 객체를 만들지 않아도 쓸 수 있고, 객체가 모두 사라져도 값이 남는 이유가 여기에 있음
+ 인스턴스 변수는 스택이 아니라 힙에 있음
    - 객체 안에 들어있기 때문. 스택에 올라가는 것은 지역 변수뿐임

### static 변수
+ 클래스에 속하는 변수로, 객체가 아닌 클래스 레벨에서 관리됨
    - 객체마다 따로 존재하지 않고, 클래스 단위로 하나만 존재
    - 모든 객체가 값을 공유하며, 공통 데이터 저장 용도로 사용
+ 주요 특징
    - 객체를 생성하지 않아도 사용 가능
    - 클래스명.변수명 형식으로 접근
    - 클래스가 로드될 때 만들어져 프로그램이 끝날 때까지 유지됨
        + 객체가 모두 사라져도 값은 그대로 남아 있음
    - 객체 참조로도 접근되지만(c1.count) 클래스 변수라는 점이 드러나지 않아 권장하지 않음

    ```java
    class Counter {
        static int count = 0; // 모든 객체가 공유하는 변수

        Counter() {
            count++;
        }
    }

    public class Main {
        public static void main(String[] args) {
            Counter c1 = new Counter();
            Counter c2 = new Counter();
            System.out.println(Counter.count);
            // 2 (모든 객체가 같은 count 공유)
        }
    }
    ```

+ 저장 위치
    - JVM 명세상으로는 Method Area에 속함
    - HotSpot 구현 기준으로는 조금 다름
        + Java 7까지는 PermGen에 저장
        + Java 8부터 클래스 메타데이터는 Metaspace(네이티브 메모리)로 옮겨졌고, static 필드의 값은 힙에 있는 Class 객체에 저장됨
    - Method Area는 명세상의 논리적인 영역이며 실제 위치는 구현마다 다름

### static 메서드
+ 객체 없이 클래스명.메서드명으로 호출
+ 인스턴스 멤버를 사용할 수 없음
    - this, super를 쓸 수 없음
    - 인스턴스 변수와 인스턴스 메서드를 직접 호출할 수 없음
    - this는 힙에 있는 특정 인스턴스를 가리키는 말인데, static 메서드는 객체 없이도 호출되므로 가리킬 대상을 정할 수 없기 때문
    - 반대로 인스턴스 메서드에서 static 멤버는 자유롭게 사용 가능
+ 오버라이딩되지 않고 하이딩(hiding)됨
    - 참조 타입 기준으로 컴파일 시점에 결정됨 (정적 바인딩)
    - 자식이 같은 이름으로 정의해도 부모 타입 참조로 호출하면 부모 것이 실행됨
    - 다형성이 적용되지 않으므로 `@Override`도 붙일 수 없음

    ```java
    class Parent { static void hello() { System.out.println("Parent"); } }
    class Child extends Parent { static void hello() { System.out.println("Child"); } }

    Parent p = new Child();
    p.hello();   // "Parent" ← 객체가 아니라 참조 타입 기준
    ```

### static 초기화 블록
+ static 변수를 초기화할 때 로직이 필요한 경우 사용
+ 클래스가 처음 로드될 때 딱 한 번만 실행됨
    - static 변수 초기화와 static 블록은 소스에 적힌 순서대로 실행됨
+ 실행 시점은 클래스 로딩의 초기화 단계
    - new로 객체를 만들 때, static 멤버에 처음 접근할 때처럼 클래스를 실제로 사용하는 시점
    - 예외: static final 컴파일 타임 상수는 컴파일 시 값이 인라인되어 클래스 초기화를 일으키지 않음

    ```java
    class Config {
        static final Map<String, String> DEFAULTS = new HashMap<>();

        static {
            DEFAULTS.put("host", "localhost");
            DEFAULTS.put("port", "8080");
        }
    }
    ```
+ 인스턴스 초기화 블록과의 차이
    - static 블록: 클래스당 한 번
    - 인스턴스 블록: 객체를 만들 때마다, 생성자 본문보다 먼저 실행

### static final 상수
+ 값이 바뀌지 않고 모든 객체가 공유하는 값
    - 이름은 대문자 스네이크 표기 (MAX_SIZE)
+ static 없이 final만 쓰면 객체마다 같은 값의 복사본이 생김
+ 참조형에 붙이면 참조만 고정됨
    - static final List라도 내부 값은 바뀔 수 있음 (add 가능)
    - 내용까지 막으려면 List.of()나 Collections.unmodifiableList()를 사용

### static 중첩 클래스
+ 바깥 클래스의 인스턴스 없이 만들 수 있음
    - new Outer.Inner()
+ 내부 클래스(non-static)와의 차이
    - 내부 클래스는 바깥 인스턴스의 참조를 암묵적으로 가짐
        + 바깥 객체가 GC 대상이 되지 못해 메모리 누수의 원인이 됨
    - 바깥 인스턴스를 쓸 일이 없다면 static을 붙이는 것이 기본

### static import
+ 클래스명 없이 static 멤버를 쓸 수 있게 함
    - import static java.lang.Math.PI;
+ 남용하면 그 이름이 어느 클래스 것인지 알 수 없어져 가독성이 떨어짐
    - 테스트 코드의 assertEquals 정도가 대표적인 사용 예

### 주의점
+ static 변수는 공유 자원이라 스레드 안전하지 않음
    - count++ 는 읽기 - 더하기 - 쓰기 3단계라 원자적이지 않음
    - 여러 스레드가 동시에 실행하면 값이 유실됨
    - AtomicInteger나 synchronized로 보호해야 함
+ 전역 상태가 되어 결합도를 높임
    - 어디서든 바꿀 수 있어 값이 언제 왜 바뀌었는지 추적하기 어려움
    - 테스트끼리 상태가 새어 테스트 순서에 따라 결과가 달라짐
+ 메모리 누수의 원인이 됨
    - static 필드는 GC Root이므로 여기서 참조하는 객체는 클래스가 언로드될 때까지 회수되지 않음
    - static Map을 캐시로 쓰면서 비우지 않으면 계속 쌓임
+ 상수, 유틸리티 메서드, 팩토리 메서드처럼 상태를 갖지 않는 용도에 쓰는 것이 안전함

### 관련 문서
+ 접근 제어자는 [자바의 접근 제어자](./자바의-접근-제어자.md) 문서 참고
+ static 필드와 GC Root의 관계는 [가비지 컬렉터](./가비지-컬렉터.md) 문서 참고
+ 클래스 로딩과 초기화 단계는 [자바 가상머신의 구조와 실행 매커니즘](./자바-가상머신의-구조와-실행-매커니즘.md) 문서 참고

## volatile 키워드

`volatile`은 **"이 변수는 여러 스레드가 같이 본다"** 고 JVM에 알리는 표시다.
락이 아니라서 아무도 기다리게 하지 않는 대신, 보장해주는 것도 **가시성과 순서까지**다.
`synchronized`의 가벼운 대체제가 아니라 **쓸 수 있는 상황이 정해져 있는 도구**로 봐야 한다.

### volatile 키워드
+ 스레드 간에 변수 값을 즉시 공유할 수 있도록 보장하는 키워드
+ CPU 캐시에 값을 묶어두지 않고, 읽을 때마다 다른 스레드가 쓴 최신 값을 보도록 강제함
+ 변수의 가시성(visibility)은 보장하지만, 연산 자체의 원자성(atomicity)은 보장하지 않음
+ 주로 플래그 변수, 상태 체크 등에 사용됨
+ 붙일 수 있는 자리
    - **필드에만** 붙음. 지역 변수·매개 변수에는 못 붙임
        + 지역 변수는 스레드마다 별도의 스택에 있어 애초에 공유되지 않기 때문
    - `final`과 **함께 쓸 수 없음** (컴파일 에러)
        + 값이 안 바뀌는데 최신 값을 볼 이유가 없어서 서로 모순됨

### volatile 키워드가 필요한 이유
+ 스레드가 메모리에서 값을 읽지 않고 자신의 CPU 캐시에 저장된 값을 사용할 경우, 다른 스레드가 바꾼 값을 인지하지 못하는 문제 발생
+ volatile을 쓰면 항상 최신 값을 메모리에서 읽게 되어 즉시 반영됨

```java
class Example implements Runnable {
    private volatile boolean running = true;
    @Override
    public void run() {
        while (running) {
        // 작업 수행 중...
        }
        System.out.println("스레드 종료");
    }

    public void stop() {
        running = false; // 다른 스레드에서 변경
    }
}

public class Main {
    public static void main(String[] args) throws InterruptedException {
        Example example = new Example();
        Thread t = new Thread(example);
        t.start();

        Thread.sleep(1000); // 1초 후 중지
        example.stop(); // running을 false로 설정
    }
}

===== 출력 =====
// 1초 뒤
스레드 종료

// volatile을 떼면
// stop()이 호출돼도 루프가 멈추지 않고 프로그램이 끝나지 않음
```

+ 이 예제가 `volatile`의 대표 용도인 이유
    - `running`은 **읽기와 쓰기만 있고 이전 값을 쓰지 않음** (`count++`처럼 계산하지 않음)
    - 쓰는 스레드가 하나뿐임
    - 이 두 조건이 맞을 때가 `volatile`을 쓸 수 있는 자리

### 값이 안 보이는 진짜 이유
+ "CPU 캐시 때문"이라고만 설명하면 절반만 맞음. 원인이 두 갈래임
+ **CPU 캐시와 스토어 버퍼**
    - 코어마다 자기 캐시가 있어서, 쓴 값이 곧바로 다른 코어에 보이지 않을 수 있음
+ **컴파일러와 JIT의 최적화** — 실제로는 이쪽이 더 자주 원인이 됨
    - 루프 안에서 안 바뀌는 값으로 판단하면 **루프 밖으로 끌어올려버림**(hoisting)

        ```java
        // 우리가 쓴 코드
        while (running) { }

        // JIT가 이렇게 바꿔도 단일 스레드 관점에서는 문제가 없음
        if (running) { while (true) { } }   // running을 다시 읽지 않음
        ```

    - 그래서 `stop()`이 값을 바꿔도 **애초에 다시 읽지를 않아** 영원히 멈추지 않음
+ 두 최적화 모두 **단일 스레드에서는 결과가 같아서** 자바가 허용하는 동작
    - JVM은 "다른 스레드가 이 값을 본다"는 사실을 알려주지 않으면 알 수 없음
    - `volatile`이 바로 그 표시
+ 그래서 이 버그의 성질
    - 최적화가 걸릴 만큼 루프가 돌아야 재현되므로 **디버거를 붙이면 멀쩡히 동작**하기도 함
    - `-Xint`(인터프리터 전용)로 돌리면 사라지기도 함

### volatile이 보장하는 것
+ **가시성 (Visibility)**
    - 쓰면 즉시 다른 스레드가 볼 수 있는 곳으로 내보냄
    - 읽을 때마다 매번 새로 읽어옴 (캐싱·호이스팅 금지)
+ **순서 (Ordering)**
    - `volatile` 변수 앞뒤의 명령을 넘나드는 재정렬을 막음 (메모리 배리어)
    - **volatile 쓰기는 이후의 volatile 읽기보다 먼저 일어난 것으로 보장됨** (happens-before)
+ 순서 보장에서 따라오는 부수 효과가 중요함
    - `volatile` 변수를 쓰기 **직전에 한 일반 변수 쓰기까지 같이 보임**

        ```java
        int data = 0;                  // 일반 변수
        volatile boolean ready = false;

        // 스레드 1
        data = 42;
        ready = true;      // volatile 쓰기 — 여기까지의 쓰기가 모두 공개됨

        // 스레드 2
        if (ready) {
            System.out.println(data);  // 반드시 42 (0이 나올 수 없음)
        }
        ```

    - 이 성질을 이용해 **플래그 하나로 앞선 데이터 전체를 안전하게 넘길 수 있음**
+ **`long`과 `double`의 읽기/쓰기 원자성**
    - 자바 명세상 일반 `long`/`double`은 32비트씩 나눠 쓰는 것이 허용됨
    - 그래서 동기화 없이 쓰면 **위쪽 절반만 반영된 이상한 값**을 볼 수 있음
    - `volatile`을 붙이면 이 분할이 사라짐

### volatile이 보장하지 않는 것
+ **복합 연산의 원자성**

    ```java
    private volatile int count = 0;
    public void increment() { count++; }   // ❌ 여전히 안전하지 않음
    ```

    - `count++`은 **읽기 → 더하기 → 쓰기** 3단계
    - `volatile`은 각 단계를 최신 값으로 만들어줄 뿐, **세 단계가 끊기지 않게 하지는 못함**
    - 두 스레드가 같은 값을 읽고 각자 1을 더하면 결과는 여전히 1만 증가
+ 원자성이 깨지는 대표 패턴
    - `count++`, `count += n` 같은 **읽고-고쳐-쓰기**
    - `if (value == null) value = new X();` 같은 **검사 후 행동(check-then-act)**
    - 서로 맞물린 **두 개 이상의 변수**를 함께 바꾸는 경우
+ 해결은 다른 도구로
    - 단일 변수 카운터라면 `AtomicInteger`
    - 여러 변수를 묶어야 하면 `synchronized`나 `Lock`

### 언제 쓸 수 있나
+ 다음 조건을 **모두** 만족할 때만 안전함
    - 쓰는 값이 **현재 값에 의존하지 않음** (증가·누적이 아님)
    - 또는 **쓰는 스레드가 하나뿐**임
    - 이 변수가 다른 변수와 **불변식으로 묶여 있지 않음**
+ 실제로 쓰이는 자리
    - 스레드 종료 플래그 (`volatile boolean running`)
    - 초기화 완료 표시 (`volatile boolean initialized`)
    - 한 스레드만 쓰고 여러 스레드가 읽는 상태 값
    - 이중 검사 잠금(DCL)의 인스턴스 필드
+ 반대로 쓰면 안 되는 자리
    - 카운터, 누적 합계
    - 잔액처럼 **읽은 값을 근거로 판단하고 다시 쓰는** 값

### synchronized와의 비교

| | `volatile` | `synchronized` |
|---|---|---|
| 대상 | **필드 하나** | 코드 블록·메서드 |
| 가시성 | 보장 | 보장 |
| 순서 | 보장 | 보장 |
| 원자성 | **보장 안 함** | 보장 |
| 블로킹 | 없음 (대기하지 않음) | 있음 (`BLOCKED`) |
| 데드락 | 발생하지 않음 | 발생 가능 |
| 비용 | 낮음 | 경합 시 높음 |

+ 관계로 정리하면
    - `synchronized`는 `volatile`이 하는 일을 **포함**함 (원자성 + 가시성 + 순서)
    - 그래서 `synchronized` 블록 안에서만 읽고 쓰는 필드에는 `volatile`이 필요 없음
+ 고르는 순서
    - 값 하나를 **그냥 쓰고 읽기만** 한다 → `volatile`
    - 값 하나를 **읽고 고쳐 쓴다** → `Atomic`
    - **여러 값을 함께** 바꾼다 → `synchronized` / `Lock`

### 참조 타입에 붙일 때의 함정
+ `volatile`이 지키는 건 **참조 자체**지 가리키는 객체의 내부가 아님

    ```java
    private volatile List<String> list = new ArrayList<>();

    list = new ArrayList<>();   // ✅ 이 재할당은 다른 스레드에 보임
    list.add("hello");          // ❌ 내부 상태 변경은 아무것도 보장되지 않음
    ```

+ 배열도 마찬가지

    ```java
    private volatile int[] arr = new int[10];

    arr = new int[20];   // ✅ 배열 참조 교체는 보임
    arr[0] = 42;         // ❌ 원소 쓰기는 보장 없음
    ```

    - 원소 단위로 필요하면 `AtomicIntegerArray`
+ 그래서 참조 타입에 `volatile`을 쓸 때는 **불변 객체를 통째로 갈아끼우는 방식**이 맞음

    ```java
    private volatile Map<String, String> config = Map.of();

    public void reload(Map<String, String> next) {
        config = Map.copyOf(next);   // 새 불변 맵으로 교체
    }
    ```

    - 읽는 쪽은 락 없이 읽고, 쓰는 쪽은 통째로 교체 — 읽기가 압도적으로 많을 때 잘 맞음

### 이중 검사 잠금 (Double-Checked Locking)
+ `volatile`이 없으면 깨지는 대표 예제

    ```java
    class Singleton {
        private static volatile Singleton instance;   // volatile 필수

        public static Singleton getInstance() {
            if (instance == null) {                   // 1차 검사 (락 없이)
                synchronized (Singleton.class) {
                    if (instance == null) {           // 2차 검사 (락 안에서)
                        instance = new Singleton();
                    }
                }
            }
            return instance;
        }
    }
    ```

+ `volatile`을 빼면 왜 깨지는가
    - `new Singleton()`은 실제로 **① 메모리 할당 → ② 생성자 실행 → ③ 참조 대입** 순
    - 재정렬이 허용되어 **③이 ②보다 먼저** 일어날 수 있음
    - 그 틈에 다른 스레드가 1차 검사를 통과해 **아직 생성자가 안 끝난 객체**를 받아감
    - `volatile`이 이 재정렬을 막아줌 (Java 5부터 유효한 보장)
+ 다만 싱글턴이 목적이라면 더 단순한 방법이 있음

    ```java
    // holder 관용구 — 클래스 로딩이 초기화를 한 번만 해주므로 동기화 코드가 아예 필요 없음
    class Singleton {
        private Singleton() { }
        private static class Holder {
            private static final Singleton INSTANCE = new Singleton();
        }
        public static Singleton getInstance() { return Holder.INSTANCE; }
    }
    ```

    - DCL은 **"왜 재정렬이 위험한가"를 보여주는 예제**로서의 가치가 더 큼

### 성능
+ 락이 아니라서 **대기가 없고 데드락도 없음**
    - 컨텍스트 스위칭이 발생하지 않음
+ 대신 최적화를 포기하는 비용이 있음
    - 읽기는 대부분 저렴함 (x86에서는 일반 읽기와 비용이 거의 같음)
    - **쓰기가 비쌈** — 스토어 버퍼를 비우는 배리어가 들어감
    - 캐싱·재정렬·호이스팅 같은 최적화가 그 변수에 대해 막힘
+ 그래서 **읽기가 많고 쓰기가 드문 값**에 가장 잘 맞음
    - 종료 플래그, 설정 스냅샷이 전형적인 예
+ 성능을 노리고 `synchronized` 자리에 끼워 넣는 용도가 아님
    - 보장 범위가 달라서 애초에 대체 관계가 아님

### 자주 하는 착각
+ "`volatile`을 붙이면 스레드 안전해진다"
    - **가시성만** 해결됨. `count++` 같은 복합 연산은 여전히 깨짐
+ "`volatile`은 가벼운 `synchronized`다"
    - 원자성을 보장하지 않아서 대체 관계가 아님. **보장 범위가 다른 도구**
+ "`volatile int count`면 카운터로 써도 된다"
    - 안 됨. 두 스레드가 같은 값을 읽고 각자 더하면 증가가 사라짐. `AtomicInteger`를 써야 함
+ "`volatile`은 락을 걸어서 느리다"
    - 락이 아니라 대기가 없음. 읽기는 거의 공짜에 가깝고 쓰기만 배리어 비용이 있음
+ "`volatile`을 쓰면 CPU 캐시를 아예 안 쓴다"
    - 캐시는 그대로 씀. 재정렬을 막고 최신 값이 보이도록 **메모리 배리어를 넣는 것**이 실제 동작
+ "값이 안 보이는 건 캐시 때문이다"
    - JIT가 루프 밖으로 값을 끌어올리는 최적화가 더 흔한 원인. 그래서 디버거를 붙이면 재현이 안 되기도 함
+ "`volatile List`면 리스트 조작도 안전하다"
    - **참조 교체만** 보장됨. `add()`, `remove()`는 아무 보장이 없음
+ "지역 변수에도 `volatile`을 붙일 수 있다"
    - 필드에만 붙음. 지역 변수는 스레드마다 따로라 공유되지 않음
+ "`synchronized`로 감싼 필드에도 `volatile`을 같이 붙이면 더 안전하다"
    - 항상 같은 락 안에서만 접근한다면 **불필요함**. `synchronized`가 이미 가시성을 보장함
+ "`volatile long`은 원자적이니 `count += 1`도 괜찮다"
    - **읽기와 쓰기 각각**이 원자적일 뿐. 읽고 더해서 쓰는 조합은 여전히 안 됨

### 관련 문서
+ 가시성·원자성·순서가 왜 문제가 되는지는 [멀티 스레딩이란 — 여기서 생기는 문제](멀티-스레딩이란.md#여기서-생기는-문제) 문서 참고
+ `synchronized`가 가시성까지 보장하는 원리는 [synchronized 키워드 — synchronized가 보장하는 것](synchronized-키워드.md#synchronized가-보장하는-것) 문서 참고
+ `volatile`로 부족한 복합 연산을 처리하는 방법은 [Atomic 클래스](atomic-클래스.md) 문서 참고
+ `final` 필드의 안전 발행은 [final 키워드 — final과 스레드 안전성](../04-자바와-객체지향-프로그래밍/final-키워드.md#final과-스레드-안전성) 문서 참고
+ JIT가 코드를 어떻게 바꾸는지는 [자바 가상머신의 구조와 실행 매커니즘 — JIT 컴파일러](../03-자바-핵심-구조/자바-가상머신의-구조와-실행-매커니즘.md#jit-just-in-time-컴파일러) 문서 참고

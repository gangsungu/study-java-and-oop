## Atomic 클래스

`java.util.concurrent.atomic` 패키지의 클래스들은 **락 없이 값 하나를 안전하게 고치는** 도구다.
`volatile`이 못 하는 원자성을 채워주면서, `synchronized`처럼 다른 스레드를 멈춰 세우지는 않는다.
핵심은 클래스가 아니라 그 밑에 깔린 **CAS**라는 아이디어다.

### 왜 필요한가
+ `volatile`과 `synchronized` 사이에 빈 자리가 있음

    | 도구 | 가시성 | 원자성 | 블로킹 |
    |---|:---:|:---:|:---:|
    | `volatile` | O | **X** | 없음 |
    | `Atomic` | O | O | **없음** |
    | `synchronized` | O | O | 있음 |

+ 문제는 늘 같은 자리에서 생김

    ```java
    private volatile int count = 0;
    public void increment() { count++; }   // ❌ 읽기 → 더하기 → 쓰기 3단계
    ```

    - `volatile`은 각 단계에서 최신 값을 보게 해줄 뿐, 세 단계를 묶어주지 못함
+ `synchronized`로 막을 수는 있지만 값 하나 올리자고 락을 잡는 게 과함
    - 락을 못 얻은 스레드가 `BLOCKED`로 내려가고 컨텍스트 스위칭이 발생함
+ `Atomic`은 **락 없이(lock-free)** 같은 일을 함

    ```java
    private final AtomicInteger count = new AtomicInteger(0);
    public void increment() { count.incrementAndGet(); }   // ✅
    ```

### CAS (Compare-And-Swap)
+ Atomic 클래스가 동작하는 원리
    - **"지금 값이 내가 알던 값과 같으면 바꾸고, 다르면 실패를 알려줘"** 를 한 번에 처리
    - CPU가 제공하는 단일 명령(x86의 `cmpxchg`)이라 중간에 끼어들 수 없음

    ```java
    // 의미상 이런 동작이 통째로 원자적으로 실행됨
    if (현재값 == 기대값) { 현재값 = 새값; return true; }
    else { return false; }
    ```

+ 실패하면 **다시 읽고 다시 시도**함 (스핀)

    ```java
    // incrementAndGet()이 내부적으로 하는 일
    int current, next;
    do {
        current = get();          // 지금 값을 읽고
        next = current + 1;       // 새 값을 계산한 뒤
    } while (!compareAndSet(current, next));  // 그 사이 안 바뀌었을 때만 성공
    ```

    - 다른 스레드가 먼저 바꿨다면 `compareAndSet`이 실패하고, 새 값으로 다시 계산함
    - **덮어쓰는 대신 다시 하는 것** — 그래서 갱신이 사라지지 않음
+ 락과의 차이

    | | 락 (`synchronized`) | CAS (`Atomic`) |
    |---|---|---|
    | 방식 | 비관적 — 일단 막고 시작 | 낙관적 — 부딪히면 다시 |
    | 대기 | `BLOCKED`로 내려감 | 멈추지 않고 재시도 |
    | 컨텍스트 스위칭 | 발생 | 없음 |
    | 경합이 심하면 | 대기가 길어짐 | **재시도가 낭비됨** |

+ 내부 구조는 결국 `volatile` + CAS
    - `AtomicInteger`의 값은 `volatile int`로 선언되어 있음 → 가시성 확보
    - 여기에 CAS로 원자성을 얹은 것
    - 예전에는 `Unsafe`로, Java 9부터는 `VarHandle`로 CAS를 호출함

### 주요 클래스

| 클래스 | 용도 |
|---|---|
| `AtomicInteger` / `AtomicLong` | 숫자 카운터 |
| `AtomicBoolean` | 플래그, 단 한 번만 실행 보장 |
| `AtomicReference<V>` | 객체 참조 교체 |
| `AtomicIntegerArray` / `AtomicLongArray` / `AtomicReferenceArray` | **배열 원소 단위** 원자적 갱신 |
| `AtomicStampedReference` / `AtomicMarkableReference` | ABA 문제 대응 |
| `LongAdder` / `LongAccumulator` | 경합이 심한 누적 (Java 8+) |

+ `volatile int[]`로는 원소 갱신이 보호되지 않음
    - 참조 교체만 보장되므로, 원소 단위가 필요하면 `AtomicIntegerArray`
+ 필드 하나를 원자적으로 다루고 싶은데 객체 수가 많아 래퍼가 부담이면
    - `AtomicIntegerFieldUpdater` 계열이 있음. Java 9+에서는 `VarHandle`이 권장됨

### 자주 쓰는 메서드

| 메서드 | 하는 일 |
|---|---|
| `get()` / `set(v)` | 읽기 / 쓰기 (`volatile`과 동일) |
| `incrementAndGet()` | `++i` — 더한 **뒤** 값 |
| `getAndIncrement()` | `i++` — 더하기 **전** 값 |
| `addAndGet(n)` / `getAndAdd(n)` | n만큼 더하기 |
| `getAndSet(v)` | 새 값을 넣고 **이전 값**을 반환 |
| `compareAndSet(expect, update)` | 기대값과 같을 때만 교체, 성공 여부 반환 |
| `updateAndGet(f)` / `getAndUpdate(f)` | 함수로 새 값 계산 (Java 8+) |
| `accumulateAndGet(x, f)` | 현재 값과 x를 함수로 합침 (Java 8+) |

```java
AtomicInteger count = new AtomicInteger(10);

count.incrementAndGet();          // 11 반환, 값은 11
count.getAndIncrement();          // 11 반환, 값은 12
count.compareAndSet(12, 100);     // true,  값은 100
count.compareAndSet(12, 999);     // false, 값은 그대로 100 (기대값이 다름)
count.updateAndGet(v -> v * 2);   // 200
```

+ `updateAndGet`/`accumulateAndGet`의 람다는 **여러 번 호출될 수 있음**
    - CAS가 실패하면 새 값으로 다시 계산하기 때문
    - 그래서 **부수 효과가 없는 순수 함수**여야 함

        ```java
        // ❌ 재시도될 때마다 로그가 중복으로 찍힘
        count.updateAndGet(v -> { log.info("증가"); return v + 1; });
        ```
+ "딱 한 번만 실행"에는 `compareAndSet`이 잘 맞음

    ```java
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    public void init() {
        if (initialized.compareAndSet(false, true)) {
            // 여러 스레드가 동시에 불러도 여기는 정확히 한 번만 실행됨
        }
    }
    ```

### 여러 값을 함께 바꿔야 할 때
+ Atomic이 보장하는 건 **변수 하나**의 원자성

    ```java
    AtomicInteger x = new AtomicInteger();
    AtomicInteger y = new AtomicInteger();

    x.incrementAndGet();   // 각각은 원자적이지만
    y.incrementAndGet();   // ❌ 둘을 합친 구간은 원자적이지 않음
    ```

    - 그 사이에 다른 스레드가 x와 y를 **어긋난 상태로 볼 수 있음**
+ 묶어야 하는 값들은 **불변 객체 하나로 만들어 `AtomicReference`에 담음**

    ```java
    record Range(int lower, int upper) { }   // 불변

    private final AtomicReference<Range> range =
            new AtomicReference<>(new Range(0, 10));

    public void setUpper(int newUpper) {
        range.updateAndGet(r -> {
            if (newUpper < r.lower()) throw new IllegalArgumentException();
            return new Range(r.lower(), newUpper);   // 통째로 새 객체 교체
        });
    }
    ```

    - 두 필드가 항상 짝이 맞는 상태로만 공개됨
    - 상태가 복잡해지거나 경합이 심하면 그냥 `synchronized`가 단순하고 나음

### ABA 문제
+ CAS는 **값만 비교**하기 때문에 생기는 함정
    - 스레드 1이 값 `A`를 읽고 계산하는 사이
    - 스레드 2가 `A → B → A`로 되돌려놓으면
    - 스레드 1의 CAS는 **아무 일도 없었던 줄 알고 성공**함
+ 값만 보면 문제가 없지만, 그 사이 상태가 바뀌었다면 버그가 됨
    - 스택·큐처럼 노드를 재사용하는 자료구조에서 실제로 문제가 됨
    - 단순 카운터라면 값이 같으면 실제로 같은 것이라 신경 쓸 일이 없음
+ 값에 **버전(스탬프)** 을 붙여 해결

    ```java
    AtomicStampedReference<String> ref = new AtomicStampedReference<>("A", 0);

    int[] stamp = new int[1];
    String value = ref.get(stamp);            // 값과 버전을 같이 읽음

    // 값과 버전이 둘 다 그대로일 때만 성공
    ref.compareAndSet(value, "C", stamp[0], stamp[0] + 1);
    ```

    - `AtomicMarkableReference`는 버전 대신 `boolean` 표시 하나만 붙임

### LongAdder (Java 8+)
+ 경합이 심할 때 `AtomicLong`의 약점
    - 모든 스레드가 **같은 변수 하나**에 CAS를 시도함
    - 스레드가 많아질수록 실패와 재시도가 늘고, 캐시 라인 경합까지 겹침
+ `LongAdder`는 **내부 셀 여러 개에 나눠 담고, 읽을 때 합침**

    ```java
    LongAdder counter = new LongAdder();

    counter.increment();       // 각 스레드가 서로 다른 셀에 더함
    counter.add(5);
    long total = counter.sum();  // 필요할 때 전부 합산
    ```

+ 성격 비교

    | | `AtomicLong` | `LongAdder` |
    |---|---|---|
    | 경합 낮음 | 좋음 | 비슷하거나 약간 손해 (메모리 더 씀) |
    | 경합 높음 | 재시도 낭비 | **훨씬 빠름** |
    | 현재 값 읽기 | 정확한 값 | `sum()` — 진행 중 갱신은 반영이 보장되지 않음 |
    | 값을 근거로 판단 | 가능 (`compareAndSet`) | **불가** |

+ 선택 기준
    - **누적만 하고 가끔 읽는 통계·카운터** → `LongAdder`
    - **값을 읽어서 판단·비교해야 함** (재고, 상한 체크) → `AtomicLong`
+ `LongAccumulator`는 덧셈 말고 다른 결합 연산(최댓값 등)을 같은 방식으로 처리함

### 언제 무엇을 쓸까

| 상황 | 도구 |
|---|---|
| 플래그를 쓰기만 하고 읽기만 함 | `volatile` |
| 값 하나를 읽고 고쳐 씀 (카운터) | `AtomicInteger`, `AtomicLong` |
| 딱 한 번만 실행되게 하고 싶음 | `AtomicBoolean.compareAndSet` |
| 참조를 통째로 교체함 | `AtomicReference` |
| 누적만 하고 가끔 읽음 (고경합) | `LongAdder` |
| 여러 변수를 함께 바꿔야 함 | `synchronized`, `ReentrantLock` |
| 대기·복잡한 조건이 필요함 | `ReentrantLock`, `BlockingQueue` |
| 공유 맵 | `ConcurrentHashMap` |

+ 판단 순서로 정리하면
    - 공유를 **안 할 수 있는가** → 지역 변수, 불변 객체, `ThreadLocal`
    - 공유해야 한다면 **바꾸는 값이 하나인가** → `Atomic`
    - 여러 값이 묶여 있는가 → 락

### 성능
+ 경합이 낮으면 CAS가 락보다 유리
    - 락은 경합이 없어도 진입·해제 비용이 있고, 경합하면 컨텍스트 스위칭까지 발생
    - CAS는 성공하면 명령 하나로 끝남
+ 경합이 심해지면 역전될 수 있음
    - 재시도가 계속 실패하면 **CPU를 태우기만 함**
    - 이때는 `LongAdder`로 분산하거나, 차라리 락으로 재우는 편이 나을 수 있음
+ 락 프리는 "기아 없음"이 아님
    - 전체적으로는 계속 진행되지만, 운 나쁜 스레드 하나가 **계속 실패할 수는 있음**
+ 결국 측정이 답
    - 스레드 수, 경합 정도, 읽기/쓰기 비율에 따라 순위가 바뀜

### 자주 하는 착각
+ "Atomic을 쓰면 그 클래스 전체가 스레드 안전해진다"
    - 보장되는 건 **그 변수 하나의 연산**뿐. 두 Atomic을 이어 쓰는 구간은 원자적이지 않음
+ "Atomic은 락을 안 쓰니 항상 빠르다"
    - 경합이 심하면 재시도가 낭비됨. 이때는 락이나 `LongAdder`가 나음
+ "`AtomicInteger`도 결국 내부에서 락을 건다"
    - CPU의 CAS 명령을 씀. 락이 아니라서 대기 상태로 내려가지 않음
+ "`getAndIncrement()`와 `incrementAndGet()`은 같다"
    - 반환값이 다름. `getAndIncrement()`는 **더하기 전 값**을 돌려줌
+ "`updateAndGet()`의 람다는 한 번만 실행된다"
    - CAS 실패 시 **여러 번 실행됨**. 로그·DB 저장 같은 부수 효과를 넣으면 안 됨
+ "`compareAndSet`이 `false`를 반환하면 오류다"
    - 그 사이 값이 바뀌었다는 정상 신호임. 다시 읽고 재시도하면 됨
+ "CAS가 성공했으면 그동안 아무 일도 없었다"
    - ABA 문제. `A → B → A`로 돌아왔어도 성공함. 필요하면 `AtomicStampedReference`
+ "`AtomicReference`에 담으면 그 객체 내부도 안전하다"
    - **참조 교체만** 원자적임. 담긴 객체가 가변이면 아무 보장이 없어서 **불변 객체를 담아야 함**
+ "`LongAdder.sum()`은 항상 정확한 값이다"
    - 합산 중 진행되는 갱신이 반영된다는 보장이 없음. 값을 근거로 판단해야 하면 `AtomicLong`
+ "`volatile`을 붙였으니 `AtomicInteger`는 필요 없다"
    - `volatile`은 원자성이 없음. `count++`을 하는 순간부터 Atomic이 필요함
+ "Atomic 필드는 `final`로 두면 안 된다"
    - 오히려 `private final`이 맞음. **참조는 고정하고 안의 값만 바꾸는 것**이 정상 사용법

### 관련 문서
+ 원자성·가시성·순서 세 문제의 배경은 [멀티 스레딩이란 — 여기서 생기는 문제](멀티-스레딩이란.md#여기서-생기는-문제) 문서 참고
+ 가시성만 필요할 때 쓰는 `volatile`은 [volatile 키워드](volatile-키워드.md) 문서 참고
+ 여러 값을 묶어 보호하는 락 방식은 [synchronized 키워드](synchronized-키워드.md) 문서 참고
+ `synchronized`의 한계와 `ReentrantLock`은 [synchronized 키워드 — synchronized의 한계](synchronized-키워드.md#synchronized의-한계) 문서 참고
+ 불변 객체를 만드는 방법은 [final 키워드 — final과 불변 객체](../04-자바와-객체지향-프로그래밍/final-키워드.md#final과-불변-객체) 문서 참고
+ 동시성 컬렉션은 [다양한 map의 구현체 비교](../05-컬렉션-프레임워크/다양한-map의-구현체-비교.md#hashtable과-concurrenthashmap) 문서 참고

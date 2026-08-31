## synchronized 키워드

`synchronized`는 **한 번에 하나의 스레드만 들어올 수 있는 구역**을 만드는 키워드다.
붙이는 자리에 따라 **무엇을 잠그는지(락 객체)** 가 달라지고, 여기서 오해가 가장 많이 생긴다.
"이 메서드를 잠근다"가 아니라 **"이 객체의 락을 잡는다"** 로 읽어야 한다.

### 동기화(synchronization)
+ 멀티 스레드 환경에서 여러 스레드가 공유 자원에 동시에 접근하는 것을 방지하는 기법
+ synchronized 키워드를 사용하여 한 번에 하나의 스레드만 특정 코드 블록 또는 메서드에 접근 가능
+ 동기화가 필요한 이유
    - count++ 같은 연산은 실제로는 읽기 → 증가 → 저장의 3단계로 이루어져 있음
    - 여러 스레드가 동시에 접근하면 데이터 덮어쓰기나 손실 발생 가능
    > synchronized로 임계 영역(Critical Section)을 보호해야 함

+ 무엇이 어긋나는지 단계로 보면

    | 시점 | 스레드 1 | 스레드 2 | count |
    |---|---|---|:---:|
    | 1 | count 읽기 → 10 | | 10 |
    | 2 | | count 읽기 → 10 | 10 |
    | 3 | 11 계산 후 저장 | | 11 |
    | 4 | | 11 계산 후 저장 | 11 |

    - 두 번 증가시켰는데 **1만 늘어남**. 이렇게 사라지는 갱신을 lost update라고 함
    - 이 어긋남은 **아주 짧은 순간에만** 발생해서, 반복 횟수가 적으면 재현되지 않음

    ```java
    class SharedResource {
        private int count = 0;
        public synchronized void increment() {
            count++;
            System.out.println("현재 count: " + count + " / 실행 스레드: " + Thread.currentThread().getName());
        }
    }

    public class Main {
        public static void main(String[] args) throws InterruptedException {
            SharedResource resource = new SharedResource();
            Runnable task = () -> {
                for (int i = 0; i < 1000; i++) {
                    resource.increment();
                }
            };

            Thread t1 = new Thread(task, "스레드 1");
            Thread t2 = new Thread(task, "스레드 2");

            t1.start();
            t2.start();

            t1.join();
            t2.join();
        }
    }

    ===== 출력 =====
    현재 count: 1 / 실행 스레드: 스레드 1
    현재 count: 2 / 실행 스레드: 스레드 1
    ...
    현재 count: 2000 / 실행 스레드: 스레드 2
    // → 중복 없이 1부터 2000까지 정확하게 증가함
    // synchronized를 떼면 1987, 1994 ... 처럼 2000보다 작은 값에서 끝남
    ```

    - 스레드 **이름의 순서는 여전히 뒤섞임**. 동기화는 순서를 정해주는 게 아니라 **겹치지 않게** 해줄 뿐

### 모니터 락 (Monitor Lock)
+ 자바의 **모든 객체는 락을 하나씩 가지고 있음**
    - 객체 헤더(Mark Word)에 락 상태가 기록됨
    - 이 락을 모니터 락, 또는 인트린식 락(intrinsic lock)이라고 부름
+ `synchronized`는 그 락을 잡았다 놓는 동작
    - 락을 이미 다른 스레드가 쥐고 있으면 `BLOCKED` 상태로 대기
    - 블록을 벗어나면 자동으로 반납됨 — **예외로 빠져나가도 반납됨**
+ 바이트코드에서의 차이
    - `synchronized` **블록** → `monitorenter` / `monitorexit` 명령으로 컴파일
    - `synchronized` **메서드** → 메서드에 `ACC_SYNCHRONIZED` 플래그만 붙고 JVM이 알아서 처리

    ```bash
    javap -c -p SharedResource.class
    ```

### 붙일 수 있는 4가지 자리
+ 자리마다 **잠기는 객체가 다름**

    | 자리 | 문법 | 락 객체 |
    |---|---|---|
    | 인스턴스 메서드 | `synchronized void m()` | `this` |
    | static 메서드 | `static synchronized void m()` | `클래스명.class` |
    | 인스턴스 블록 | `synchronized (obj) { }` | `obj` |
    | 클래스 블록 | `synchronized (Xxx.class) { }` | 클래스 객체 |

    ```java
    class Counter {
        // 아래 둘은 완전히 같은 의미
        public synchronized void a() { }
        public void b() { synchronized (this) { } }

        // 아래 둘도 완전히 같은 의미
        public static synchronized void c() { }
        public static void d() { synchronized (Counter.class) { } }
    }
    ```

+ **인스턴스 락과 클래스 락은 별개**
    - `a()`를 실행 중인 스레드가 있어도 다른 스레드는 `c()`에 들어갈 수 있음
    - 서로 다른 락이라 막지 못함. 같은 데이터를 건드린다면 버그가 됨
+ **인스턴스가 다르면 락도 다름**

    ```java
    Counter c1 = new Counter();
    Counter c2 = new Counter();
    // c1.a() 와 c2.a() 는 서로를 전혀 막지 않음 (락 객체가 다르므로)
    ```

    - 그래서 "이 메서드는 한 번에 하나만 실행된다"가 아니라 **"이 객체당 하나씩만 실행된다"** 가 맞는 표현

### 메서드보다 블록
+ `synchronized` 메서드는 **메서드 전체**가 임계 영역이 됨
    - 락을 쥔 시간이 길어질수록 다른 스레드가 그만큼 오래 대기함
+ 정말 보호가 필요한 부분만 블록으로 감싸는 편

    ```java
    // ❌ I/O가 임계 영역 안에 들어가 락을 오래 쥠
    public synchronized void increment() {
        count++;
        System.out.println("현재 count: " + count);  // 느린 작업
    }

    // ✅ 공유 상태를 만지는 구간만 잠근다
    public void increment() {
        int current;
        synchronized (this) {
            current = ++count;
        }
        System.out.println("현재 count: " + current); // 락 밖에서 출력
    }
    ```

    - 위 예제가 느린 이유도 이것 — `println`은 그 자체로 동기화된 I/O라 임계 영역에 넣으면 병목이 됨
+ 임계 영역 안에서 하면 안 되는 것
    - I/O, 네트워크 호출, `sleep()`
    - **다른 객체의 락을 추가로 잡는 일** — 데드락의 출발점
    - 외부에서 넘겨받은 콜백 호출 — 그 안에서 무슨 락을 잡을지 알 수 없음

### 전용 락 객체 관용구
+ `this`를 락으로 쓰면 **바깥에서도 그 객체로 락을 걸 수 있음**

    ```java
    Counter counter = new Counter();
    synchronized (counter) {
        // 외부 코드가 Counter의 모든 synchronized 메서드를 통째로 막아버림
    }
    ```

+ 그래서 외부에 노출되지 않는 **전용 락 객체**를 두는 편

    ```java
    class Counter {
        private final Object lock = new Object();   // private + final
        private int count = 0;

        public void increment() {
            synchronized (lock) { count++; }
        }
    }
    ```

    - `private` — 외부가 이 락을 잡을 수 없음
    - `final` — 락 객체가 중간에 **바뀌지 않음** (바뀌면 상호배제가 깨짐)
+ 보호할 자원이 여러 개면 락도 나눠 두면 경합이 줄어듦 (lock splitting)

### 락으로 쓰면 안 되는 객체
+ **String 리터럴**

    ```java
    synchronized ("lock") { }   // ❌
    ```

    - 리터럴은 상수 풀에서 **JVM 전체가 공유**함. 전혀 관계없는 코드가 같은 락을 잡게 됨
+ **박싱 타입 (`Integer`, `Long`, `Boolean` ...)**

    ```java
    private Integer count = 0;
    synchronized (count) { count++; }   // ❌
    ```

    - `-128 ~ 127`은 캐시된 객체를 공유함
    - 게다가 `count++`를 하면 **새 객체가 만들어져 락 객체 자체가 바뀜**
+ **재할당되는 필드**
    - 락 객체가 바뀌는 순간, 서로 다른 락을 잡은 스레드들이 동시에 들어옴
    - 락으로 쓸 참조는 반드시 `final`
+ 정리하면 락 객체의 조건은 **`private`, `final`, 공유되지 않는 것**

### synchronized가 보장하는 것
+ **원자성 (Atomicity)**
    - 임계 영역 안의 코드가 중간에 끼어들기 당하지 않음
+ **가시성 (Visibility)**
    - 락을 **잡을 때** 다른 스레드가 바꾼 값을 메인 메모리에서 다시 읽어옴
    - 락을 **놓을 때** 자신이 바꾼 값을 메인 메모리로 내보냄
    - 그래서 `synchronized`는 `volatile`이 하는 일까지 포함함
+ **순서 (Ordering)**
    - 앞선 스레드의 unlock이 뒤 스레드의 lock보다 먼저 일어난 것으로 보장됨 (happens-before)
+ 여기서 나오는 규칙 하나
    - **쓰기만 동기화하고 읽기를 빼먹으면 소용없음**

        ```java
        class Flag {
            private boolean done = false;
            public synchronized void set() { done = true; }
            public boolean isDone() { return done; }   // ❌ 가시성 보장 없음
        }
        ```

    - 같은 데이터에 접근하는 **모든 경로**가 같은 락을 잡아야 의미가 있음. 한 곳이라도 빠지면 전부 무효

### 재진입 (Reentrant)
+ 같은 스레드는 **이미 자신이 쥔 락을 다시 잡을 수 있음**
    - 락마다 소유 스레드와 획득 횟수를 세고 있어서, 나갈 때 하나씩 줄이다가 0이 되면 반납

    ```java
    class Service {
        public synchronized void a() {
            b();   // 이미 this 락을 쥐고 있으므로 그냥 들어감
        }
        public synchronized void b() { }
    }
    ```

+ 재진입이 안 됐다면 자기 자신을 기다리는 데드락이 났을 것
+ 상속에서도 같음
    - 자식의 `synchronized` 메서드가 `super.method()`를 호출해도 같은 `this` 락이라 문제없음

### wait() / notify() / notifyAll()
+ 락을 잡은 채로 **조건이 만족될 때까지 기다려야 할 때** 사용
+ 세 메서드 모두 `Object`의 메서드
    - **반드시 해당 객체의 `synchronized` 안에서 호출**해야 함. 아니면 `IllegalMonitorStateException`

    | 메서드 | 하는 일 |
    |---|---|
    | `wait()` | **락을 반납하고** `WAITING` 상태로 대기 |
    | `notify()` | 대기 중인 스레드 **하나**를 깨움 (누구인지 지정 불가) |
    | `notifyAll()` | 대기 중인 스레드 **전부**를 깨움 |

+ `sleep()`과의 결정적 차이는 **락 반납 여부**
    - `sleep()`은 락을 쥔 채로 잠들고, `wait()`는 락을 놓고 기다림
    - 그래서 `sleep()`으로 조건을 기다리면 아무도 조건을 바꿀 수 없어 영원히 멈춤
+ 조건 검사는 반드시 `while`

    ```java
    class Buffer {
        private final Queue<String> queue = new LinkedList<>();
        private static final int MAX = 10;

        public synchronized void put(String item) throws InterruptedException {
            while (queue.size() == MAX) {   // ⚠ if가 아니라 while
                wait();                      // 락을 놓고 대기
            }
            queue.add(item);
            notifyAll();                     // 꺼내려고 기다리던 쪽을 깨움
        }

        public synchronized String take() throws InterruptedException {
            while (queue.isEmpty()) {
                wait();
            }
            String item = queue.poll();
            notifyAll();
            return item;
        }
    }
    ```

    - `if`면 안 되는 이유
        + 깨어나서 락을 다시 잡기까지 시간이 있어, 그 사이 **다른 스레드가 조건을 이미 소모**했을 수 있음
        + 아무도 깨우지 않았는데 깨어나는 **가짜 기상(spurious wakeup)** 이 명세상 허용됨
    - 그래서 깨어난 뒤 **조건을 다시 확인**해야 함
+ `notify()`보다 `notifyAll()`
    - `notify()`는 누구를 깨울지 고를 수 없어서, 조건이 안 맞는 스레드만 깨우고 끝날 수 있음
    - 그러면 아무도 진행하지 못한 채 모두 멈춤(신호 유실)
+ 실무에서는 이 조합을 직접 쓸 일이 많지 않음
    - `BlockingQueue` 같은 표준 구현이 이미 이 패턴을 안전하게 담고 있음

### 데드락 (Deadlock)
+ 두 스레드가 **서로 상대가 쥔 락을 기다리며** 둘 다 멈춘 상태

    ```java
    // 스레드 1
    synchronized (lockA) {
        synchronized (lockB) { }   // lockB를 기다림
    }

    // 스레드 2
    synchronized (lockB) {
        synchronized (lockA) { }   // lockA를 기다림
    }
    ```

+ 성립 조건 네 가지 (하나만 깨도 안 생김)
    - **상호 배제** — 한 번에 하나만 쓸 수 있음
    - **점유 대기** — 가진 채로 다른 것을 기다림
    - **비선점** — 뺏을 수 없음
    - **순환 대기** — 대기 관계가 원을 그림
+ 실무에서 쓰는 예방책
    - **락 획득 순서를 전역으로 통일** — 항상 A → B 순서로만 잡으면 순환이 생기지 않음
    - **중첩 락을 피함** — 락 하나로 끝나면 데드락도 없음
    - `ReentrantLock.tryLock(시간)` — 정해진 시간 안에 못 얻으면 포기하고 물러남
+ 진단
    - `jstack <pid>` 또는 `jcmd <pid> Thread.print`가 `Found one Java-level deadlock`으로 알려줌
    - 스레드가 `BLOCKED` 상태로 멈춰 있으면 락 대기를 의심

### synchronized의 한계
+ 못 하는 것들

    | 한계 | 설명 |
    |---|---|
    | 타임아웃 불가 | 락을 얻을 때까지 **무한 대기**. 포기할 방법이 없음 |
    | 인터럽트 불가 | 락 대기(`BLOCKED`) 중인 스레드는 `interrupt()`로 깨울 수 없음 |
    | 시도 불가 | "잡히면 하고 아니면 넘어가기"가 안 됨 |
    | 공정성 불가 | 오래 기다린 스레드가 먼저 얻는다는 보장이 없음 |
    | 읽기/쓰기 구분 불가 | 읽기만 하는 스레드끼리도 서로 막음 |

    - `wait()`로 대기 중인 스레드는 인터럽트가 먹힘. **락을 기다리는 `BLOCKED`만 안 먹힘**
+ 대안

    | 상황 | 대안 |
    |---|---|
    | 타임아웃·인터럽트·공정성이 필요 | `ReentrantLock` |
    | 읽기가 압도적으로 많음 | `ReadWriteLock`, `StampedLock` |
    | 단순 카운터·플래그 | `AtomicInteger`, `AtomicLong` (CAS 기반, 락 없음) |
    | 가시성만 필요 (단일 쓰기) | `volatile` |
    | 공유 컬렉션 | `ConcurrentHashMap`, `CopyOnWriteArrayList`, `BlockingQueue` |

    - `ReentrantLock`은 `finally`에서 `unlock()`을 **직접 해야 함**. 이 부담이 없다는 게 `synchronized`의 장점
    - `volatile`과 `Atomic`은 별도 문서로 정리 예정

### 성능
+ 경합이 없으면 생각보다 저렴함
    - HotSpot은 상황에 따라 락을 단계적으로 올림: **무락 → 경량 락(스핀) → 중량 락(OS 뮤텍스)**
    - 경량 락 단계에서는 잠깐 스핀하며 기다리므로 OS까지 안 내려감
    - 편향 락(biased locking)은 Java 15에서 deprecated, Java 18에서 제거됨
+ 비싸지는 건 **경합이 생겼을 때**
    - 중량 락으로 넘어가면 스레드가 `BLOCKED`로 내려가고 컨텍스트 스위칭이 발생
+ 그래서 최적화 방향은 "동기화를 없애기"보다 **경합을 줄이기**
    - 임계 영역을 짧게
    - 락을 쪼개기 (자원별로 다른 락)
    - 애초에 공유하지 않기 — 불변 객체, 지역 변수, `ThreadLocal`
+ 미리 최적화할 이유는 없음
    - 정확성이 먼저고, 병목이 확인된 뒤에 바꾸는 순서가 맞음

### 자주 하는 착각
+ "`synchronized`를 붙이면 그 변수가 보호된다"
    - 보호되는 건 변수가 아니라 **락 객체**임. 같은 데이터를 만지는 다른 경로에 락이 없으면 아무 의미 없음
+ "인스턴스 메서드와 static 메서드에 둘 다 `synchronized`면 서로 막는다"
    - 락이 `this`와 `클래스.class`로 **달라서 동시에 실행됨**
+ "`synchronized` 메서드는 한 번에 하나의 스레드만 실행한다"
    - **인스턴스마다** 락이 따로임. 객체가 2개면 2개가 동시에 돌아감
+ "읽기(getter)에는 동기화가 필요 없다"
    - 가시성 때문에 필요함. 쓰기만 동기화하면 읽는 쪽은 낡은 값을 계속 볼 수 있음
+ "`synchronized`는 원자성만 보장한다"
    - 가시성과 순서(happens-before)까지 보장함. `volatile`이 하는 일을 포함함
+ "`synchronized`를 쓰면 실행 순서가 정해진다"
    - **겹치지 않게** 할 뿐, 누가 먼저 들어갈지는 정해지지 않음
+ "`wait()`는 `sleep()`처럼 잠깐 쉬는 것이다"
    - `wait()`는 **락을 반납**함. 이게 결정적인 차이
+ "`wait()`를 `if`로 감싸도 된다"
    - 가짜 기상과 조건 선점 때문에 반드시 `while`
+ "`notify()`가 내가 원하는 스레드를 깨운다"
    - 임의의 하나를 깨움. 조건이 안 맞는 스레드만 깨우면 전부 멈출 수 있어 `notifyAll()`이 안전
+ "락을 기다리는 스레드는 `interrupt()`로 깨울 수 있다"
    - `BLOCKED` 상태는 인터럽트가 안 먹힘. 깨우고 싶으면 `ReentrantLock.lockInterruptibly()`
+ "예외가 나면 락이 안 풀린다"
    - 풀림. `monitorexit`가 예외 경로에도 들어가서 블록을 벗어나면 반납됨
+ "`synchronized`는 무조건 느리다"
    - 경합이 없으면 저렴함. 느려지는 건 여러 스레드가 실제로 부딪힐 때

### 관련 문서
+ 원자성·가시성·순서 세 가지 문제의 배경은 [멀티 스레딩이란 — 여기서 생기는 문제](멀티-스레딩이란.md#여기서-생기는-문제) 문서 참고
+ `BLOCKED`, `WAITING` 등 스레드 상태는 [멀티 스레딩이란 — 스레드 생명주기](멀티-스레딩이란.md#스레드-생명주기) 문서 참고
+ `sleep()`이 락을 반납하지 않는 이유는 [멀티 스레딩이란 — 스레드 제어](멀티-스레딩이란.md#스레드-제어) 문서 참고
+ 락 객체를 `final`로 잡아야 하는 이유와 안전 발행은 [final 키워드 — final과 스레드 안전성](../04-자바와-객체지향-프로그래밍/final-키워드.md#final과-스레드-안전성) 문서 참고
+ `static` 가변 상태가 동기화 대상이 되는 이유는 [static 키워드 — 주의점](../03-자바-핵심-구조/static-키워드.md#주의점) 문서 참고
+ `Hashtable`처럼 통째로 잠그는 방식과 `ConcurrentHashMap`의 차이는 [다양한 map의 구현체 비교](../05-컬렉션-프레임워크/다양한-map의-구현체-비교.md#hashtable과-concurrenthashmap) 문서 참고

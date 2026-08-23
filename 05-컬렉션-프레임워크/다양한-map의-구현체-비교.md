## 다양한 map의 구현체 비교

### 한눈에 보기
| | HashMap | LinkedHashMap | TreeMap | Hashtable | ConcurrentHashMap |
|---|---|---|---|---|---|
| 내부 구조 | 해시 테이블 | 해시 테이블 + 연결 리스트 | 레드-블랙 트리 | 해시 테이블 | 해시 테이블 (구간별 락) |
| 순서 | 보장 안 함 | **삽입/접근 순서** | **키 정렬 순서** | 보장 안 함 | 보장 안 함 |
| 성능 | O(1) | O(1) | O(log N) | O(1), 락 비용 | O(1) |
| `null` 키 | 1개 허용 | 1개 허용 | **불가** | **불가** | **불가** |
| `null` 값 | 허용 | 허용 | 허용 | **불가** | **불가** |
| 스레드 안전 | X | X | X | O (전체 락) | O (부분 락) |
| 언제 쓰나 | 기본 선택 | 순서를 보여줘야 할 때 | 정렬·범위 조회 | 쓰지 않음 (레거시) | 멀티스레드 |

+ `Set` 구현체와 대응 관계가 그대로임 (`HashSet`↔`HashMap`, `TreeSet`↔`TreeMap` …)
    - 실제로 `HashSet`은 내부에서 `HashMap`을 쓰고 있음

### HashMap
+ 키-값 쌍을 저장하며, 키는 중복 불가·값은 중복 가능
+ 해시 기반이라 조회·삽입·삭제가 모두 O(1)에 가까움
+ 순서를 보장하지 않음
    ```java
    Map<Integer, String> map = new HashMap<>();
    map.put(3, "C");
    map.put(1, "A");
    map.put(2, "B");
    System.out.println(map); // 출력 예시: {3=C, 1=A, 2=B} (순서 보장 X)
    ```
+ **동작 방식**
    1. 키의 `hashCode()`로 저장할 버킷(배열 칸) 위치를 계산
    2. 같은 버킷에 이미 값이 있으면(**해시 충돌**) 그 안에서 `equals()`로 비교
    3. 충돌한 항목은 연결 리스트로 이어지고, 한 버킷에 8개 이상 쌓이면 **트리로 전환**되어 O(log N)으로 방어
+ **리사이징**: 저장된 개수가 `용량 × 로드 팩터(기본 0.75)`를 넘으면 배열 크기를 2배로 늘리고 전부 재배치
    - 담을 개수를 미리 알면 `new HashMap<>(1000)`처럼 초기 용량을 주는 게 유리
+ 키는 `equals()`와 `hashCode()`를 **둘 다** 제대로 재정의해야 함
    - 안 하면 분명히 `put`한 키인데 `get`이 `null`을 반환함
+ `null` 키를 **하나** 허용함 (0번 버킷에 저장)

### LinkedHashMap
+ 삽입 순서를 유지하는 `HashMap`
+ `HashMap`을 상속받고, 엔트리들을 이중 연결 리스트로 엮어 순서를 기억함
    ```java
    Map<Integer, String> map = new LinkedHashMap<>();
    map.put(3, "C");
    map.put(1, "A");
    map.put(2, "B");
    System.out.println(map); // 출력: {3=C, 1=A, 2=B} (입력 순서 유지) 

    ```
+ 조회 성능은 `HashMap`과 동일한 O(1), 링크 유지 비용만 추가됨
+ **접근 순서 모드(access-order)** 를 켜면 최근에 조회한 항목이 뒤로 감
    ```java
    // 초기 용량, 로드 팩터, accessOrder
    Map<Integer, String> map = new LinkedHashMap<>(16, 0.75f, true);
    map.put(1, "A"); map.put(2, "B"); map.put(3, "C");
    map.get(1);                       // 1번을 조회
    System.out.println(map);          // 출력: {2=B, 3=C, 1=A}
    ```
    - `removeEldestEntry()`를 재정의하면 **LRU 캐시**가 됨 (가장 오래 안 쓴 항목 자동 제거)
+ 응답 JSON의 필드 순서, 설정값 출력 순서처럼 **순서가 눈에 보이는 곳**에서 씀

### TreeMap
+ Key 기준 정렬이 자동으로 수행됨
+ Comparable 또는 Comparator를 사용해 정렬 방식 변경 가능
+ 이진 탐색 트리 기반으로 O(log N) 성능
    ```java
    Map<Integer, String> map = new TreeMap<>();
    map.put(3, "C");
    map.put(1, "A");
    map.put(2, "B");
    System.out.println(map); // 출력: {1=A, 2=B, 3=C} (Key 기준 정렬)
    ```
+ 정렬 기준을 바꾸려면 생성자에 `Comparator`를 넘김
    ```java
    Map<String, Integer> map = new TreeMap<>(Comparator.reverseOrder());
    ```
+ **정렬돼 있어야만 가능한 조회**가 진짜 강점 (`NavigableMap`)
    ```java
    TreeMap<Integer, String> grades = new TreeMap<>();
    grades.put(90, "A"); grades.put(80, "B"); grades.put(70, "C");

    grades.firstKey();          // 70
    grades.lastEntry();         // 90=A
    grades.floorEntry(85);      // 80=B - 85 이하 중 가장 큰 키
    grades.ceilingEntry(85);    // 90=A - 85 이상 중 가장 작은 키
    grades.headMap(85);         // {70=C, 80=B}
    grades.subMap(75, 95);      // {80=B, 90=A}
    grades.descendingMap();     // {90=A, 80=B, 70=C}
    ```
    - "점수 구간별 등급", "시간 범위 조회"처럼 **경계를 찾는 문제**에 잘 맞음
+ 키가 `Comparable`이 아니면 **실행 시점**에 `ClassCastException`
+ 키에 `null`을 넣을 수 없음 (비교가 불가능하므로 `NullPointerException`)

### Hashtable과 ConcurrentHashMap
+ **`Hashtable`** — `HashMap`의 옛날 버전
    - 모든 메서드에 `synchronized`가 붙어 있어 스레드 하나만 접근 가능 → 느림
    - 키·값 모두 `null` 불가
    - **새 코드에서는 쓰지 않음.** 동기화가 필요하면 `ConcurrentHashMap`
+ **`ConcurrentHashMap`** — 멀티스레드용 `Map`
    - 맵 전체가 아니라 **버킷 단위로만 락**을 걸어 동시 접근을 허용 → 훨씬 빠름
    - 키·값 모두 `null` 불가 (값이 `null`인 건지 키가 없는 건지 구분할 수 없어서)
    - `putIfAbsent()`, `compute()` 같은 **원자적 연산**을 제공
+ `Collections.synchronizedMap(map)`도 있지만 맵 전체에 락을 거는 방식이라 `ConcurrentHashMap`보다 느림

### 자주 쓰는 메서드
```java
Map<String, Integer> count = new HashMap<>();

// 키가 없으면 기본값 - null 체크가 사라진다
int c = count.getOrDefault("a", 0);

// 키가 없을 때만 넣기
count.putIfAbsent("a", 1);

// 개수 세기 (없으면 1, 있으면 +1)
count.merge("a", 1, Integer::sum);

// 값이 컬렉션일 때 - 없으면 만들어서 반환
Map<String, List<String>> group = new HashMap<>();
group.computeIfAbsent("fruit", k -> new ArrayList<>()).add("apple");

// 순회는 entrySet()이 가장 효율적 (키로 다시 조회하지 않음)
for (Map.Entry<String, Integer> e : count.entrySet()) {
    System.out.println(e.getKey() + " = " + e.getValue());
}
count.forEach((k, v) -> System.out.println(k + " = " + v));
```
+ `merge()`와 `computeIfAbsent()`를 알면 "값이 있는지 확인하고 없으면 만들고..."류의 코드가 한 줄로 줄어듦
    ```java
    // 이렇게 쓰던 코드가
    if (!map.containsKey(key)) map.put(key, new ArrayList<>());
    map.get(key).add(value);

    // 이렇게 된다
    map.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
    ```

### 선택 기준
```
멀티스레드에서 공유하나?
├─ 예 → ConcurrentHashMap
└─ 아니오 → 순서가 필요한가?
            ├─ 아니오          → HashMap
            ├─ 넣은 순서 그대로 → LinkedHashMap
            └─ 키 정렬 / 범위 조회 → TreeMap
```
+ 기본은 `HashMap`. 순서나 정렬이 **실제로 요구사항일 때만** 다른 걸 고름
+ 단순히 결과를 정렬해 보여주는 게 목적이면 `HashMap` + `stream().sorted()`가 더 빠를 수 있음
    - `TreeMap`은 **넣을 때마다** 정렬 비용을 냄

### 자주 하는 착각
+ "`get()`이 `null`을 반환하면 키가 없는 것이다"
    - 값 자체가 `null`일 수도 있음. 구분하려면 `containsKey()`를 써야 함
+ "`HashMap`은 스레드 세이프하다"
    - 아님. 멀티스레드에서 동시에 리사이징이 일어나면 데이터가 유실되거나 무한 루프에 빠질 수 있음
+ "`Hashtable`을 쓰면 동기화가 해결된다"
    - 개별 메서드만 원자적임. `if (!map.containsKey(k)) map.put(k, v)` 같은 **복합 연산은 여전히 안전하지 않음**
+ "`HashMap`의 순서는 매번 랜덤이다"
    - 해시값에 따른 고정된 순서. 다만 **자바 버전이나 요소 개수에 따라 바뀔 수 있으니** 의존하면 안 됨
+ "키로 쓴 객체의 필드를 나중에 바꿔도 된다"
    - 해시값이 바뀌어 **그 키로는 영영 조회가 안 됨**. 키는 불변 객체(`String`, `Integer`, `record`)를 쓰는 게 원칙
        ```java
        Map<User, String> map = new HashMap<>();
        User u = new User("kim");
        map.put(u, "값");
        u.setId("lee");
        System.out.println(map.get(u)); // 출력: null (분명 넣었는데도)
        ```
+ "`keySet()`으로 순회하면서 `map.remove()`를 해도 된다"
    - `ConcurrentModificationException`이 남. `entrySet().removeIf()`나 `Iterator.remove()`를 써야 함
+ "`TreeMap`은 값(value) 기준으로도 정렬된다"
    - **키만** 정렬함. 값 기준 정렬이 필요하면 `entrySet()`을 스트림으로 정렬해야 함
        ```java
        map.entrySet().stream()
           .sorted(Map.Entry.comparingByValue())
           .forEach(e -> System.out.println(e));
        ```

### 관련 문서
+ `Map`이 `Collection`이 아닌 이유는 [컬렉션 3대장 구조와 특징의 이해](컬렉션-3대장-구조와-특징의-이해.md#상속-구조) 문서 참고
+ `HashSet`이 `HashMap`을 쓰는 방식은 [다양한 set의 구현체 비교](다양한-set의-구현체-비교.md#hashset) 문서 참고
+ `equals()`와 `hashCode()`의 관계는 [기본형과 참조형 타입의 차이](../03-자바-핵심-구조/기본형과-참조형-타입의-차이.md#-와-equals의-차이) 문서 참고
+ 키를 불변 객체로 만드는 방법은 [final 키워드](../04-자바와-객체지향-프로그래밍/final-키워드.md) 문서 참고

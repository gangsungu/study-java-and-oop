## 다양한 set의 구현체 비교

### 한눈에 보기
| | HashSet | LinkedHashSet | TreeSet |
|---|---|---|---|
| 내부 구조 | 해시 테이블 (`HashMap`) | 해시 테이블 + 연결 리스트 | 균형 이진 탐색 트리 (레드-블랙 트리) |
| 순서 | 보장 안 함 | **삽입 순서** 유지 | **정렬 순서** 유지 |
| 성능 (add/contains/remove) | O(1) | O(1) | O(log N) |
| `null` 저장 | 1개 허용 | 1개 허용 | **불가** (`NullPointerException`) |
| 필요한 조건 | `equals()`/`hashCode()` | `equals()`/`hashCode()` | `Comparable` 또는 `Comparator` |
| 언제 쓰나 | 기본 선택 | 순서를 보여줘야 할 때 | 정렬·범위 검색이 필요할 때 |

+ 셋 다 `Set` 인터페이스 구현체라 **중복을 허용하지 않는다**는 성질은 같음
+ 차이는 오직 **순서**와 **성능**뿐이므로, 선택 기준도 이 둘로 갈림

### HashSet
+ 순서 없음, 중복 허용 안됨
+ 검색, 추가, 삭제 속도가 빠름 (O(1))
+ 해시 기반으로 동작
+ 데이터를 저장할 때 특정 규칙(해시함수)을 통해 메모리 위치를 빠르게 계산
    ```java
    Set<String> set = new HashSet<>();
    set.add("B");
    set.add("A");
    set.add("C");
    System.out.println(set); // 출력 예시: [A, C, B] (순서 보장 X)
    ```
+ 내부는 사실 `HashMap`
    - 값을 키로 쓰고, 값 자리에는 의미 없는 더미 객체를 넣음
    - 그래서 `HashMap`의 특성(해시 충돌, 로드 팩터, 리사이징)을 그대로 물려받음
+ 중복 판정은 **`hashCode()`로 위치를 찾고 → `equals()`로 비교**하는 2단계
    - 둘 중 하나만 재정의하면 중복 제거가 깨짐 ([컬렉션 3대장](컬렉션-3대장-구조와-특징의-이해.md#컬렉션이-같음을-판단하는-방법) 참고)
+ `add()`는 **추가 성공 여부를 `boolean`으로 반환** → 중복 검사에 그대로 쓸 수 있음
    ```java
    if (!visited.add(node)) {
        return; // 이미 방문한 노드
    }
    ```

### LinkedHashSet
+ 입력된 순서를 유지하는 HashSet
+ HashSet보다 약간 느리지만, 순서 유지가 필요할 때 사용
+ 중복은 여전히 허용하지 않음
    ```java
    Set<String> set = new LinkedHashSet<>();
    set.add("B");
    set.add("A");
    set.add("C");
    System.out.println(set); // 출력: [B, A, C] (입력 순서 유지)
    ```
+ `HashSet`을 상속받고, 요소들을 **이중 연결 리스트로 한 번 더 엮어** 순서를 기억함
    - 조회 성능은 `HashSet`과 같은 O(1). 링크를 유지하는 만큼 메모리를 조금 더 씀
+ **재삽입은 순서를 바꾸지 않음**
    ```java
    Set<String> set = new LinkedHashSet<>(List.of("A", "B", "C"));
    set.add("A"); // 이미 있는 값 → 무시됨
    System.out.println(set); // 출력: [A, B, C] (A가 뒤로 가지 않음)
    ```
+ 순서가 결과에 드러나는 곳(응답 JSON, 로그, 화면 출력)에서 특히 유용
    - 중복 제거 후 **원래 순서 그대로** 보여줘야 할 때의 정답

### TreeSet
+ 자동 정렬 (기본: 오름차순, Comparable or Comparator 사용 가능)
+ 이진 탐색 트리 기반으로 O(log N) 성능
+ 중복 불가능, 정렬이 필요한 경우 사용
    ```java
    Set<String> set = new TreeSet<>();
    set.add("B");
    set.add("A");
    set.add("C");
    System.out.println(set); // 출력: [A, B, C] (자동 정렬)
    ```
+ 정렬 기준을 직접 주려면 생성자에 `Comparator`를 넘김
    ```java
    Set<String> desc = new TreeSet<>(Comparator.reverseOrder());
    desc.addAll(List.of("B", "A", "C"));
    System.out.println(desc); // 출력: [C, B, A]
    ```
+ **중복 판정 기준이 다름**: `equals()`가 아니라 **비교 결과가 `0`인지**로 판단
    - 그래서 정렬 기준이 엉성하면 서로 다른 객체가 중복으로 취급돼 사라짐
        ```java
        Set<String> byLength = new TreeSet<>(Comparator.comparingInt(String::length));
        byLength.add("apple");
        byLength.add("melon"); // 길이가 같아 중복 취급 → 저장 안 됨
        System.out.println(byLength); // 출력: [apple]
        ```
    - 해결: `Comparator.comparingInt(String::length).thenComparing(Comparator.naturalOrder())`처럼 **동점을 깨는 기준**을 붙임
+ 정렬돼 있기 때문에 **범위·경계 조회**를 지원하는 게 진짜 강점 (`NavigableSet`)
    ```java
    TreeSet<Integer> scores = new TreeSet<>(List.of(10, 30, 50, 70, 90));
    scores.first();           // 10  - 최솟값
    scores.last();            // 90  - 최댓값
    scores.ceiling(40);       // 50  - 40 이상 중 가장 작은 값
    scores.floor(40);         // 30  - 40 이하 중 가장 큰 값
    scores.headSet(50);       // [10, 30]     - 50 미만
    scores.tailSet(50);       // [50, 70, 90] - 50 이상
    scores.subSet(30, 70);    // [30, 50]     - 30 이상 70 미만
    scores.descendingSet();   // [90, 70, 50, 30, 10]
    ```
    - 이런 조회가 필요 없다면 굳이 `TreeSet`을 쓸 이유가 없음
+ `Comparable`을 구현하지 않은 객체를 넣으면 **컴파일이 아니라 실행 시점**에 `ClassCastException`

### 선택 기준
```
중복만 없으면 되나?
├─ 예 → 순서도 필요한가?
│        ├─ 아니오          → HashSet
│        ├─ 넣은 순서 그대로 → LinkedHashSet
│        └─ 정렬된 순서      → TreeSet
└─ 정렬 + 범위 검색(최솟값, 이상/이하)이 필요하다 → TreeSet
```
+ 실무에서는 `HashSet`이 압도적으로 많고, 출력 순서가 눈에 보일 때만 `LinkedHashSet`으로 바꾸는 흐름
+ "정렬된 결과가 필요하다"는 이유만으로는 `TreeSet`이 정답이 아닐 수 있음
    - 한 번만 정렬하면 되는 경우엔 `HashSet`에 모은 뒤 `stream().sorted()`가 더 빠름
    - `TreeSet`은 **삽입할 때마다** 정렬 비용을 냄

### 자주 하는 착각
+ "`HashSet`은 순서가 랜덤하다"
    - 랜덤이 아니라 **해시값 기반의 예측하기 어려운 순서**. 같은 데이터를 같은 순서로 넣으면 출력도 같음. 다만 그 순서에 의존하면 안 됨
+ "`Set`이니까 `get(0)`으로 꺼낼 수 있다"
    - `Set`에는 인덱스 접근 자체가 없음. 순회하거나 `List`로 변환해야 함
+ "`TreeSet`은 `equals()`로 중복을 판단한다"
    - `compareTo()`/`compare()`의 반환값이 `0`이면 중복. `equals()`와 결과가 어긋나면 예상 밖의 동작을 함
+ "`HashSet`에 `null`을 넣으면 에러가 난다"
    - `HashSet`·`LinkedHashSet`은 `null` 하나를 허용함. **`TreeSet`이 `null`에서 `NullPointerException`** (비교를 할 수 없어서)
+ "`Set`에 넣은 객체의 필드를 나중에 바꿔도 된다"
    - 해시값이 바뀌어 **원래 버킷에서 영영 못 찾게 됨**. `contains()`가 `false`를 반환하는데 `size()`는 그대로인 상태가 됨
        ```java
        Set<User> set = new HashSet<>();
        User u = new User("kim");
        set.add(u);
        u.setId("lee");            // 해시값이 바뀜
        System.out.println(set.contains(u)); // 출력: false (분명 들어있는데도)
        ```
    - Set의 키가 되는 객체는 **불변으로 두는 게 원칙**
+ "`LinkedHashSet`은 `TreeSet`처럼 정렬해준다"
    - 정렬이 아니라 **삽입 순서** 유지. 넣은 순서가 뒤죽박죽이면 출력도 뒤죽박죽

### 관련 문서
+ `Set`이 `Collection` 계층 어디에 있는지는 [컬렉션 3대장 구조와 특징의 이해](컬렉션-3대장-구조와-특징의-이해.md) 문서 참고
+ 내부 구현인 `HashMap`의 동작은 [다양한 map의 구현체 비교](다양한-map의-구현체-비교.md) 문서 참고
+ `equals()`와 `hashCode()`의 관계는 [기본형과 참조형 타입의 차이](../03-자바-핵심-구조/기본형과-참조형-타입의-차이.md#-와-equals의-차이) 문서 참고
+ 불변 객체로 만드는 방법은 [final 키워드](../04-자바와-객체지향-프로그래밍/final-키워드.md) 문서 참고

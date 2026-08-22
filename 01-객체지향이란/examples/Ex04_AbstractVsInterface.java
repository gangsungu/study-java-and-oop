/*
 * [문서 대응] 01-객체지향이란/객체지향이란.md > "추상클래스 vs 인터페이스"
 *
 * "A는 B이다(is-a)" -> 추상클래스 / "A는 B를 할 수 있다(can-do)" -> 인터페이스
 *
 * 실행: java 01-객체지향이란/examples/Ex04_AbstractVsInterface.java
 */
import java.util.List;

public class Ex04_AbstractVsInterface {
    public static void main(String[] args) {

        System.out.println("=== 1. 추상클래스: 공통 '상태'와 '구현'을 물려준다 ===");
        Duck duck = new Duck("도날드");
        Fish fish = new Fish("니모");
        duck.sleep();     // Animal이 구현해 둔 코드를 그대로 사용 (name 필드까지 물려받음)
        fish.sleep();
        duck.speak();     // 미완성이던 부분은 자식이 채웠다
        fish.speak();
        System.out.println("""
                해설: name 필드와 sleep() 구현은 Animal이 한 번만 작성했다.
                     이런 '공통 상태 + 공통 구현' 물려주기는 인터페이스로는 못 한다.
                """);

        System.out.println("=== 2. 인터페이스: 계층이 달라도 같은 행동으로 묶는다 ===");
        // Duck과 Fish는 Animal의 자식이지만, Submarine은 Animal과 아무 관계가 없다.
        // 그래도 셋 다 '헤엄칠 수 있다'는 계약을 지키므로 하나로 묶인다.
        List<Swimmable> swimmers = List.of(duck, fish, new Submarine("SSN-774"));
        for (Swimmable s : swimmers) {
            s.swim();
            s.floatOnWater();     // default 메서드 - 구현하지 않아도 공짜로 따라온다
        }
        System.out.println("""
                해설: Submarine은 Animal을 상속하지 않는다. 상속으로는 절대 한 리스트에
                     담을 수 없는 것들을 인터페이스는 '행동'만 보고 묶어준다.
                """);

        System.out.println("=== 3. 다중 구현: 하나의 클래스가 여러 계약을 지킬 수 있다 ===");
        // 클래스 상속은 하나뿐(extends Animal)이지만 인터페이스는 몇 개든 가능하다.
        duck.swim();
        duck.fly();
        System.out.println("Duck is Animal?    " + (duck instanceof Animal));
        System.out.println("Duck can swim?     " + (duck instanceof Swimmable));
        System.out.println("Duck can fly?      " + (duck instanceof Flyable));
        System.out.println("Fish can fly?      " + (fish instanceof Flyable));
        System.out.println();

        System.out.println("=== 4. default 메서드가 충돌하면? ===");
        // Swimmable과 Flyable 둘 다 move()라는 default 메서드를 가지고 있다.
        // 그냥 두면 "class Duck inherits unrelated defaults" 컴파일 에러가 난다.
        // -> Duck이 직접 재정의하고 Swimmable.super.move() 로 골라 써야 한다.
        duck.move();
        System.out.println("=> 다중 구현의 대가: 충돌 시 자식이 명시적으로 선택해야 한다.");
        System.out.println();

        System.out.println("=== 5. 인터페이스의 static 메서드와 상수 ===");
        System.out.println("Swimmable.MAX_DEPTH = " + Swimmable.MAX_DEPTH + "m (자동으로 public static final)");
        System.out.println(Swimmable.describe());
        System.out.println();

        System.out.println("=== 6. 정리 ===");
        System.out.println("""
                Animal(추상클래스) : 오리도 물고기도 '동물이다'      -> is-a, 단일 상속, 필드 O
                Swimmable(인터페이스): 오리도 잠수함도 '헤엄칠 수 있다' -> can-do, 다중 구현, 필드 X
                고민될 때: 물려줄 '상태'가 있으면 추상클래스, '행동 규약'만이면 인터페이스.
                """);
    }
}

// ── 추상클래스: 상태(필드) + 생성자 + 구현된 메서드를 가질 수 있다 ──────────
abstract class Animal {
    protected String name;              // 인터페이스는 이런 인스턴스 필드를 못 가진다

    public Animal(String name) {        // 인터페이스는 생성자도 없다
        this.name = name;
    }

    public void sleep() {               // 구현된 공통 메서드
        System.out.println(name + "이(가) 잠을 잡니다.");
    }

    public abstract void speak();       // 미완성 -> 자식이 반드시 구현
}

// ── 인터페이스: "이 행동을 할 수 있다"는 계약 ────────────────────────────
interface Swimmable {
    int MAX_DEPTH = 200;                // public static final이 자동으로 붙는다

    void swim();                        // public abstract 생략됨

    default void floatOnWater() {       // default: 기본 구현을 제공 (자바 8+)
        System.out.println("    ...물에 뜬다");
    }

    default void move() {
        System.out.println("    헤엄쳐서 이동");
    }

    static String describe() {          // static: 인터페이스 자체가 가진 유틸리티
        return "Swimmable: 물에서 이동할 수 있는 모든 것의 계약";
    }
}

interface Flyable {
    void fly();

    default void move() {
        System.out.println("    날아서 이동");
    }
}

// ── 다중 구현: extends는 하나, implements는 여러 개 ─────────────────────
class Duck extends Animal implements Swimmable, Flyable {
    public Duck(String name) {
        super(name);
    }

    @Override
    public void speak() {
        System.out.println(name + ": 꽥꽥");
    }

    @Override
    public void swim() {
        System.out.println(name + ": 헤엄친다");
    }

    @Override
    public void fly() {
        System.out.println(name + ": 날아간다");
    }

    // Swimmable.move()와 Flyable.move()가 충돌 -> 반드시 직접 재정의해야 함
    @Override
    public void move() {
        System.out.print("  오리는 헤엄을 우선: ");
        Swimmable.super.move();     // 원하는 쪽의 default 구현을 골라 호출
    }
}

class Fish extends Animal implements Swimmable {
    public Fish(String name) {
        super(name);
    }

    @Override
    public void speak() {
        System.out.println(name + ": (뻐끔)");
    }

    @Override
    public void swim() {
        System.out.println(name + ": 지느러미로 헤엄친다");
    }
}

// ── Animal이 아니어도 Swimmable 계약만 지키면 같은 타입으로 다뤄진다 ──────
class Submarine implements Swimmable {
    private String code;

    public Submarine(String code) {
        this.code = code;
    }

    @Override
    public void swim() {
        System.out.println(code + ": 스크류로 항행한다 (최대 " + MAX_DEPTH + "m)");
    }

    @Override
    public void floatOnWater() {        // default 메서드를 덮어쓸 수도 있다
        System.out.println("    ...부력 탱크를 비워 부상한다");
    }
}

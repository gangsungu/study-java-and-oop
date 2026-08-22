/*
 * [문서 대응] 01-객체지향이란/객체지향이란.md > "상속과 생성자 규칙"
 *
 * 생성자 호출 순서 / 자동 super() 삽입 / this() 체이닝을 눈으로 확인한다.
 *
 * 실행: java 01-객체지향이란/examples/Ex02_ConstructorRules.java
 */
public class Ex02_ConstructorRules {
    public static void main(String[] args) {

        System.out.println("=== 1. 호출은 자식 -> 부모, 실행은 부모 -> 자식 ===");
        System.out.println("[new Puppy() 실행]");
        new Puppy();
        System.out.println("""
                해설: 우리가 부른 건 Puppy 생성자 하나뿐인데 LivingThing부터 실행됐다.
                     Puppy 생성자의 첫 줄에서 super()가 부모를 먼저 호출하고,
                     그 부모도 자기 부모를 호출하며 꼭대기까지 올라간 뒤
                     맨 위(LivingThing)부터 차례로 완료되며 내려온다. (1층부터 짓는 집)
                """);

        System.out.println("=== 2. this()로 같은 클래스의 다른 생성자 재활용 ===");
        System.out.println("[new Cat() - 인자 없음]");
        new Cat();
        System.out.println("[new Cat(\"나비\") - 인자 있음]");
        new Cat("나비");
        System.out.println("""
                해설: Animal에는 Animal(String)밖에 없어서 super()를 자동으로 넣을 수 없다.
                     Cat()은 this("이름 없음")으로 Cat(String)에게 넘기고,
                     Cat(String)이 super(name)으로 부모를 직접 호출해 문제를 해결한다.
                """);

        System.out.println("=== 3. 흔한 함정: 생성자 안에서 오버라이드된 메서드 호출 ===");
        System.out.println("[new Child() 실행]");
        Child c = new Child();
        System.out.println("생성이 끝난 뒤 다시 호출: ");
        c.print();
        System.out.println("""
                해설: 부모 생성자가 돌 때 자식의 필드는 아직 초기화 전(null/0)이다.
                     그래서 같은 print()인데 생성 중에는 null, 생성 후에는 값이 보인다.
                     => 생성자 안에서는 오버라이드 가능한 메서드를 호출하지 말 것.
                """);
    }
}

// ── 1. 3단 상속으로 보는 생성자 실행 순서 ────────────────────────────────
class LivingThing {
    public LivingThing() {
        System.out.println("  1) LivingThing 생성자 (맨 위 조상이 가장 먼저 완료)");
    }
}

class DogBase extends LivingThing {
    public DogBase() {
        // 첫 줄에 super()도 this()도 없다 -> 컴파일러가 super()를 자동으로 넣어준다.
        System.out.println("  2) DogBase 생성자");
    }
}

class Puppy extends DogBase {
    public Puppy() {
        // 여기도 super() 자동 삽입. 그래서 DogBase -> LivingThing까지 줄줄이 올라간다.
        System.out.println("  3) Puppy 생성자 (마지막에 완료)");
    }
}

// ── 2. 부모에 인자 없는 생성자가 없을 때: super(...) / this(...)로 해결 ──
class Animal {
    // 생성자를 '하나라도' 직접 썼으므로 기본 생성자 Animal()은 자동 생성되지 않는다.
    public Animal(String name) {
        System.out.println("  Animal(String) 실행: " + name);
    }
}

class Cat extends Animal {
    public Cat() {
        this("이름 없음");   // ③ 같은 클래스의 Cat(String) 호출 (super()는 못 씀)
        System.out.println("  Cat() 완료 - 이름 없는 고양이");
    }

    public Cat(String name) {
        super(name);         // ② 부모 Animal(String)을 직접 호출
        System.out.println("  Cat(String) 완료 - 이름 있는 고양이");
    }
}

// ── 3. 생성자 안에서 오버라이드된 메서드를 부르면 생기는 일 ───────────────
class Parent {
    public Parent() {
        System.out.print("  부모 생성자에서 print() 호출 -> ");
        print();   // 실제로는 Child.print()가 실행된다 (동적 바인딩)
    }

    public void print() {
        System.out.println("Parent.print()");
    }
}

class Child extends Parent {
    private String message = "자식 필드 초기화 완료";

    public Child() {
        // super()가 자동 삽입되어 Parent 생성자가 먼저 끝난 뒤
        // 그 다음에야 message 필드가 초기화된다.
        System.out.print("  자식 생성자에서 print() 호출 -> ");
        print();
    }

    @Override
    public void print() {
        System.out.println("Child.print(), message = " + message);
    }
}

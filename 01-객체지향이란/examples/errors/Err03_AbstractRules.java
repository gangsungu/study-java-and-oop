/*
 * [문서 대응] 01-객체지향이란/객체지향이란.md > "추상클래스 vs 인터페이스"
 *
 * ⚠ 이 파일은 '일부러' 컴파일에 실패한다. 에러가 2개 난다.
 * 실행: javac 01-객체지향이란/examples/errors/Err03_AbstractRules.java
 *
 * 예상 에러:
 *   ① Animal is abstract; cannot be instantiated
 *   ② Cat is not abstract and does not override abstract method speak() in Animal
 */
public class Err03_AbstractRules {
    public static void main(String[] args) {
        // ① 추상클래스는 '미완성 설계도'이므로 그 자체로는 객체를 만들 수 없다.
        //    (Animal 타입 변수를 두는 것은 가능하지만, new Animal(...)은 불가)
        Animal a = new Animal("이름");
        a.speak();
    }
}

abstract class Animal {
    protected String name;

    public Animal(String name) {   // 추상클래스도 생성자는 가진다 (자식이 super로 호출)
        this.name = name;
    }

    public abstract void speak();
}

// ② abstract 메서드를 구현하지 않았다.
//    "speak()를 구현하든지, 아니면 Cat도 abstract로 선언하든지" 둘 중 하나여야 한다.
class Cat extends Animal {
    public Cat(String name) {
        super(name);
    }
}

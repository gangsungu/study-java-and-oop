/*
 * [문서 대응] 01-객체지향이란/객체지향이란.md > "추상클래스 vs 인터페이스" (다중 구현)
 *
 * ⚠ 이 파일은 '일부러' 컴파일에 실패한다.
 * 실행: javac 01-객체지향이란/examples/errors/Err02_DefaultConflict.java
 *
 * 예상 에러:
 *   class Duck inherits unrelated defaults for move() from types Swimmable and Flyable
 */
public class Err02_DefaultConflict {
    public static void main(String[] args) {
        new Duck().move();
    }
}

interface Swimmable {
    default void move() { System.out.println("헤엄쳐서 이동"); }
}

interface Flyable {
    default void move() { System.out.println("날아서 이동"); }
}

// 두 인터페이스가 똑같은 시그니처의 default 메서드를 갖고 있다.
// 자바는 "어느 쪽을 쓸지" 스스로 결정하지 않는다 -> 구현 클래스가 직접 정해야 한다.
class Duck implements Swimmable, Flyable {
    // 해결: move()를 재정의하고 Swimmable.super.move(); 처럼 명시적으로 고를 것
}

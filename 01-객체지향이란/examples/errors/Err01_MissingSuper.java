/*
 * [문서 대응] 01-객체지향이란/객체지향이란.md > "자동 super() 삽입 규칙" / "기본 생성자 자동 생성 규칙"
 *
 * ⚠ 이 파일은 '일부러' 컴파일에 실패한다. 에러 메시지를 직접 보는 것이 목적.
 * 실행: javac 01-객체지향이란/examples/errors/Err01_MissingSuper.java
 *
 * 예상 에러:
 *   constructor Animal in class Animal cannot be applied to given types;
 *   required: java.lang.String / found: no arguments
 */
public class Err01_MissingSuper {
    public static void main(String[] args) {
        new Cat();
    }
}

class Animal {
    // 생성자를 '하나라도' 직접 썼기 때문에 컴파일러는 기본 생성자 Animal()을
    // 더 이상 만들어주지 않는다.
    public Animal(String name) {
        System.out.println("Animal: " + name);
    }
}

class Cat extends Animal {
    public Cat() {
        // 첫 줄에 super(...)도 this(...)도 없다
        // -> 컴파일러가 super()를 자동으로 삽입
        // -> 그런데 Animal()이 존재하지 않으므로 컴파일 에러!
        System.out.println("Cat 생성");
    }
}

/*
 * 해결 방법 3가지 (하나만 적용해도 컴파일된다)
 *  ① Animal에 public Animal() {} 추가
 *  ② Cat 생성자 첫 줄에 super("이름 없음");
 *  ③ Cat(String) 생성자를 만들고 Cat()에서 this("이름 없음"); 호출
 */

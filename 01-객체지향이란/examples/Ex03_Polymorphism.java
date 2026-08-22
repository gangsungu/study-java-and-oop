/*
 * [문서 대응] 01-객체지향이란/객체지향이란.md > "다형성 자세히 보기"
 *
 * 업캐스팅 / 동적 바인딩 / 오버라이딩 vs 오버로딩 / 다운캐스팅+instanceof
 *
 * 실행: java 01-객체지향이란/examples/Ex03_Polymorphism.java
 */
import java.util.List;

public class Ex03_Polymorphism {
    public static void main(String[] args) {

        // ── 1. 업캐스팅: Cat -> Animal (자동, 형변환 표시 불필요) ──────────
        System.out.println("=== 1. 업캐스팅 & 동적 바인딩 ===");
        Animal a = new Cat("나비");           // 선언 타입 Animal, 실제 객체 Cat
        a.speak();                            // 실제 객체(Cat)의 speak()가 실행된다
        System.out.println("선언 타입: Animal / 실제 객체: " + a.getClass().getSimpleName());
        System.out.println();

        List<Animal> zoo = List.of(new Cat("나비"), new Dog("바둑이"), new Cat("치즈"));
        for (Animal animal : zoo) {
            animal.speak();                   // 한 줄이지만 셋 다 다르게 동작
        }
        System.out.println();

        // ── 2. 선언 타입이 '할 수 있는 일'의 범위를 정한다 ─────────────────
        System.out.println("=== 2. 업캐스팅하면 자식 고유 기능은 안 보인다 ===");
        // a.scratch();  // 컴파일 에러! Animal 타입에는 scratch()가 없다.
        System.out.println("a.speak()는 되지만 a.scratch()는 컴파일 에러 (주석 참고)");
        System.out.println("정리: 어떤 메서드를 '부를 수 있나'는 선언 타입이,");
        System.out.println("      그 메서드가 '어떻게 동작하나'는 실제 객체가 결정한다.");
        System.out.println();

        // ── 3. 오버라이딩(런타임) vs 오버로딩(컴파일 타임) ────────────────
        System.out.println("=== 3. 오버라이딩 vs 오버로딩 ===");
        Cat realCat = new Cat("나비");
        Animal upcastCat = realCat;           // 같은 객체, 선언 타입만 다름

        System.out.print("describe(realCat)   -> ");
        describe(realCat);                    // 컴파일러가 describe(Cat) 선택
        System.out.print("describe(upcastCat) -> ");
        describe(upcastCat);                  // 같은 객체인데 describe(Animal) 선택!
        System.out.println("""
                해설: 오버로딩은 '컴파일 타임에 선언 타입'으로 결정된다.
                     같은 객체를 넘겨도 변수의 타입이 Animal이면 Animal 버전이 호출된다.
                     반면 오버라이딩(speak)은 '런타임에 실제 객체'로 결정된다.
                """);

        // ── 4. 필드는 오버라이딩되지 않는다 (자주 헷갈리는 지점) ───────────
        System.out.println("=== 4. 메서드는 재정의되지만 필드는 가려질 뿐 ===");
        Cat cat = new Cat("나비");
        Animal asAnimal = cat;
        System.out.println("cat.category       = " + cat.category);       // Cat 것
        System.out.println("asAnimal.category  = " + asAnimal.category);  // Animal 것!
        System.out.println("cat.getCategory()  = " + cat.getCategory());  // 메서드는 재정의됨
        System.out.println("=> 필드는 선언 타입을 따라간다. 상태는 반드시 메서드로 접근할 것.");
        System.out.println();

        // ── 5. 다운캐스팅과 instanceof ────────────────────────────────────
        System.out.println("=== 5. 다운캐스팅 & instanceof ===");
        for (Animal animal : zoo) {
            animal.speak();
            if (animal instanceof Cat) {              // 확인 후
                Cat c = (Cat) animal;                 // 다운캐스팅
                c.scratch();                          // Cat 고유 기능 사용
            }
        }
        System.out.println();

        System.out.println("[Java 16+ 패턴 매칭 - 확인과 캐스팅을 한 번에]");
        for (Animal animal : zoo) {
            if (animal instanceof Dog d) {            // 검사 + 형변환 + 변수 선언
                d.fetch();
            }
        }
        System.out.println();

        System.out.println("[instanceof 없이 캐스팅하면?]");
        Animal dog = new Dog("바둑이");
        try {
            Cat wrong = (Cat) dog;                    // 컴파일은 통과, 실행 시 폭발
            wrong.scratch();
        } catch (ClassCastException e) {
            System.out.println("ClassCastException 발생 -> " + e.getMessage());
        }
        System.out.println("=> 그래서 다운캐스팅 전에는 반드시 instanceof로 확인한다.");
    }

    // ── 오버로딩: 이름은 같고 매개변수 타입이 다른 별개의 메서드 ───────────
    static void describe(Animal animal) {
        System.out.println("[오버로딩] describe(Animal) 호출됨");
    }

    static void describe(Cat cat) {
        System.out.println("[오버로딩] describe(Cat) 호출됨");
    }
}

class Animal {
    protected String name;
    String category = "동물";                   // 부모의 필드

    public Animal(String name) {
        this.name = name;
    }

    public void speak() {
        System.out.println(name + ": ...");
    }

    public String getCategory() {
        return category;
    }
}

class Cat extends Animal {
    String category = "고양이";                 // 부모 필드를 '가리는(hiding)' 새 필드

    public Cat(String name) {
        super(name);
    }

    // ── 오버라이딩: 시그니처가 완전히 같고, 런타임에 실제 객체 기준으로 선택됨
    @Override
    public void speak() {
        System.out.println(name + ": 야옹~");
    }

    @Override
    public String getCategory() {
        return category;
    }

    // Cat에만 있는 기능 -> Animal 타입 변수로는 호출할 수 없다
    public void scratch() {
        System.out.println("    (" + name + "이(가) 스크래처를 긁습니다)");
    }
}

class Dog extends Animal {
    public Dog(String name) {
        super(name);
    }

    @Override
    public void speak() {
        System.out.println(name + ": 멍멍!");
    }

    public void fetch() {
        System.out.println("    (" + name + "이(가) 공을 물어옵니다)");
    }
}

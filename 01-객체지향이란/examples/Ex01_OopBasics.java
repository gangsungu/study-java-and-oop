/*
 * [문서 대응] 01-객체지향이란/객체지향이란.md > "객체 지향의 특징과 장점"
 *
 * 4대 특징(추상화·캡슐화·상속·다형성)이 하나의 프로그램에서 어떻게 맞물리는지 확인한다.
 *
 * 실행: java 01-객체지향이란/examples/Ex01_OopBasics.java
 */
public class Ex01_OopBasics {
    public static void main(String[] args) {

        System.out.println("=== 1. 상속: Cat/Dog는 sleep()을 직접 만들지 않았다 ===");
        Cat nabi = new Cat("나비", true, 3);
        Dog badugi = new Dog("바둑이", "진돗개");
        nabi.sleep();       // Animal에서 물려받은 코드가 그대로 동작
        badugi.sleep();     // Dog는 재정의했으므로 자기 버전이 동작
        System.out.println();

        System.out.println("=== 2. 다형성: 타입은 Animal 하나인데 소리는 제각각 ===");
        // 업캐스팅 - Cat도 Dog도 '동물'이므로 Animal 배열에 함께 담긴다.
        Animal[] zoo = { nabi, badugi, new Cat("치즈", false, 1) };
        for (Animal animal : zoo) {
            animal.speak();   // 호출 코드는 단 한 줄. 실제 실행은 객체마다 다르다.
        }
        System.out.println();

        System.out.println("=== 3. 확장성: 새 동물을 추가해도 위 for문은 그대로다 ===");
        // Cow 클래스만 새로 만들면 끝. 기존 반복문은 한 글자도 고치지 않는다.
        Animal cow = new Cow("얼룩이");
        for (Animal animal : new Animal[]{ nabi, badugi, cow }) {
            animal.speak();
        }
        System.out.println();

        System.out.println("=== 4. 캡슐화: 잘못된 값이 객체 안으로 못 들어온다 ===");
        System.out.println(nabi.getName() + "의 현재 나이: " + nabi.getAge());
        nabi.setAge(4);
        System.out.println("생일이 지나 " + nabi.getAge() + "살이 되었습니다.");
        try {
            nabi.setAge(-1);      // 필드가 public이었다면 이 방어가 불가능했다
        } catch (IllegalArgumentException e) {
            System.out.println("차단됨 -> " + e.getMessage());
        }
        System.out.println("나이는 여전히 안전한 값: " + nabi.getAge());
    }
}

// ── 추상화 ──────────────────────────────────────────────────────────────
// 실제 동물의 수많은 특징 중 프로그램에 필요한 것만 뽑아냈다.
// "이름을 가진다", "소리를 낸다", "잠을 잔다" 셋만 남기고 나머지는 버린 것이 추상화.
abstract class Animal {
    protected String name;

    public Animal(String name) {
        this.name = name;
    }

    // 소리 내는 방식은 동물마다 다르므로 여기서는 정의하지 않는다(= 미완성 설계도).
    public abstract void speak();

    // 잠자는 방식은 공통이므로 부모가 구현해서 물려준다.
    public void sleep() {
        System.out.println(name + "이(가) 잠을 잡니다.");
    }

    public String getName() {
        return name;
    }
}

// ── 상속 ────────────────────────────────────────────────────────────────
// Animal의 name 필드와 sleep()을 그대로 물려받는다. Cat은 speak()만 새로 쓰면 된다.
class Cat extends Animal {

    // ── 캡슐화 ──────────────────────────────────────────────────────────
    // private이라 외부에서 cat.age = -5; 같은 잘못된 값을 절대 넣을 수 없다.
    private boolean isIndoor;
    private int age;

    public Cat(String name, boolean isIndoor, int age) {
        super(name);
        this.isIndoor = isIndoor;
        setAge(age);          // 생성자에서도 검증 로직을 재사용
    }

    public boolean isIndoor() {
        return isIndoor;
    }

    // setter가 "문지기" 역할을 한다. 이것이 캡슐화의 실익.
    public void setAge(int age) {
        if (age < 0) {
            throw new IllegalArgumentException("나이는 음수가 될 수 없습니다: " + age);
        }
        this.age = age;
    }

    public int getAge() {
        return age;
    }

    // ── 다형성 ──────────────────────────────────────────────────────────
    @Override
    public void speak() {
        System.out.println(name + ": 야옹~");
    }
}

class Dog extends Animal {
    private String breed;

    public Dog(String name, String breed) {
        super(name);
        this.breed = breed;
    }

    public String getBreed() {
        return breed;
    }

    @Override
    public void speak() {
        System.out.println(name + ": 멍멍!");
    }

    // 부모의 sleep()을 자식이 다르게 정의할 수도 있다(선택 사항).
    @Override
    public void sleep() {
        System.out.println(name + "이(가) 배를 보이고 잠을 잡니다.");
    }
}

// 나중에 추가된 클래스. 기존 코드를 수정하지 않고 기능만 늘어난다.
class Cow extends Animal {
    public Cow(String name) {
        super(name);
    }

    @Override
    public void speak() {
        System.out.println(name + ": 음메~");
    }
}

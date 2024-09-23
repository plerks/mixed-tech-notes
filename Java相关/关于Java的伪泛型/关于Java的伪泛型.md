### Java的伪泛型
参考<https://www.cnblogs.com/robothy/p/13949788.html>:

---
Java的泛型通过擦除的方式来实现。声明了泛型的.java源代码，在编译生成.class文件之后，泛型相关的信息就消失了。可以认为，源代码中泛型相关的信息，就是提供给编译器用的。泛型信息对Java编译器可以见，对Java虚拟机不可见。

Java编译器通过如下方式实现擦除：
* 用Object或者界定类型替代泛型，产生的字节码中只包含了原始的类，接口和方法；
* 在恰当的位置插入强制转换代码来确保类型安全；
* 在继承了泛型类或接口的类中插入桥接方法来保留多态性。

Java官方文档[原文](https://docs.oracle.com/javase/tutorial/java/generics/erasure.html):
* Replace all type parameters in generic types with their bounds or Object if the type parameters are unbounded. The produced bytecode, therefore, contains only ordinary classes, interfaces, and methods.
* Insert type casts if necessary to preserve type safety.
* Generate bridge methods to preserve polymorphism in extended generic types.
---

### 伪泛型导致的上下界通配符的限制
参考:
* <https://blog.csdn.net/jdsjlzx/article/details/70479227>
* <https://zhuanlan.zhihu.com/p/363658509>
* <https://www.jianshu.com/p/2bf15c5265c5>

先说结论：以List<> list为例:
* 下界`<? super T>`可以往里存T或T的子类，往外取只能赋值给Object对象。
* 上界`<? extends T>`不能往里存，往外取可以赋值给T或T的父类。

以List<> list为例，如下Java代码:
```Java
package test;

import java.util.ArrayList;
import java.util.List;

public class GeometricObject {
    public static void main(String[] args) {
        List<? super Square> list = new ArrayList<GeometricObject>();

        // 报错The method add(capture#1-of ? super Square) in the type List<capture#1-of ? super Square> is not applicable for the arguments (GeometricObject)
        list.add(new GeometricObject());

        // ok
        list.add(new Square());

        // Type mismatch: cannot convert from capture#3-of ? super Square to GeometricObject
        GeometricObject geometricObject = list.get(0);

        // Type mismatch: cannot convert from capture#4-of ? super Square to Square
        Square square = list.get(0);

        // ok
        Object obj = list.get(0);
    }
}

class Square extends GeometricObject {

}
```
为什么会出现如上的情况？首先，`<? super Square>`并不是代表任意一种Square的父类，而是**某一种**Square父类（**未知的特定的某一种**）。Java语言的设计目标是要保证类型安全的，但是对于Java的泛型，运行时没有泛型信息来检测类型安全，所以泛型的类型安全只能编译时来做。

以list.add(new GeometricObject());这行为例，编译器符号表中list的类型信息是`List<? super Square>`，编译器要保证类型安全，所以list.add(new GeometricObject());时要进行类型安全检查，list.add(E e)期望的参数类型是泛型E，这里实际是`<? super Square>`，而`<? super Square>`代表**特定的某一种Square父类XClass**，但**具体是哪种编译器不知道**，从而new GeometricObject()可能不是XClass类(也即GeometricObject类不是XClass类或XClass的子类)，所以new GeometricObject()和`<? super Square>`对不上，Java编译器为了类型安全，于是保守地在这里编译不通过。其它的报错也可以抓住编译器只知道`<? super Square>`是未知的特定的某一种Square父类这一关键点分析出来。

上面的代码中，`List<? super Square> list = new ArrayList<GeometricObject>();`一行，看起来Java编译器是能识别出list的实际类型`ArrayList<GeometricObject>`的，但是实际应该还是要在运行时识别实际类型。例如，`List<? super Square> list = Global.createSquareList();`这里这个Global.createSquareList()是个外部的方法，内部new了个ArrayList，但是方法返回时按List返回，Java的编译逻辑应该也是像c一样单文件编译.java文件生成目标代码（.class文件）的，只是要先把一个类依赖的类编译了知道那个类的属性方法返回值类型等等信息（c里通过头文件），这样一来，根本无法在编译时知道外部的Global.createSquareList()返回的实际类型，只能按照声明类型来检查。而Java运行时又没泛型信息。从而Java只能编译时做保守策略。

总之，这里`<? super Square>`能往里存Square或Square的子类，但get后只能赋值给Object类。

再如这个代码:
```Java
package test;

import java.util.ArrayList;
import java.util.List;

public class GeometricObject {
    public static void main(String[] args) {
        List<? extends Square> list = new ArrayList<Square>();

        // 报错The method add(capture#1-of ? extends Square) in the type List<capture#1-of ? extends Square> is not applicable for the arguments (GeometricObject)
        list.add(new GeometricObject());

        // 报错The method add(capture#2-of ? extends Square) in the type List<capture#2-of ? extends Square> is not applicable for the arguments (Square)
        list.add(new Square());

        // ok
        GeometricObject geometricObject = list.get(0);

        // ok
        Square square = list.get(0);

        // ok
        Object obj = list.get(0);

        // 报错Type mismatch: cannot convert from capture#6-of ? super Square to Person
        Person person = list.get(0);
    }
}

class Square extends GeometricObject {

}

class Person {

}
```
这里`<? extends Square>`，只知道是某个Square子类，但是具体是哪种不知道，因此add任何类型都不安全，但是get出来要赋值时只要是Square或Square的父类就是可以安全赋值的。

### C++里的template
C++的泛型的能力比Java强，模板参数会根据实际的类型走一遍编译器生成实例化的类及函数，不需要这种extends和super。

### Java伪泛型的实现方式
参考

<https://docs.oracle.com/javase/tutorial/java/generics/erasure.html>

<https://docs.oracle.com/javase/tutorial/java/generics/genTypes.html>


Generics were introduced to the Java language to provide tighter type checks at compile time and to support generic programming. To implement generics, the Java compiler applies type erasure to:

* Replace all type parameters in generic types with their bounds or Object if the type parameters are unbounded. The produced bytecode, therefore, contains only ordinary classes, interfaces, and methods.

* Insert type casts if necessary to preserve type safety.

* Generate bridge methods to preserve polymorphism in extended generic types.

Type erasure ensures that no new classes are created for parameterized types; consequently, generics incur no runtime overhead.

1.擦除类型，然后用bound替换，这里的bound应该指上界。例如，`class Node<T> {...}`，编译时T被擦除为Object；`class Node<T extends Number> {...}`，编译时T被擦除为Number；`class Node<T super Number> {...}`应该也是擦除为Object。

[参考链接](https://docs.oracle.com/javase/tutorial/java/generics/genTypes.html)里的例子：
```Java
public class Node<T> {

    private T data;
    private Node<T> next;

    public Node(T data, Node<T> next) {
        this.data = data;
        this.next = next;
    }

    public T getData() { return data; }
    // ...
}
```
擦除为：
```Java
public class Node {

    private Object data;
    private Node next;

    public Node(Object data, Node next) {
        this.data = data;
        this.next = next;
    }

    public Object getData() { return data; }
    // ...
}
```

```Java
public class Node<T extends Comparable<T>> {

    private T data;
    private Node<T> next;

    public Node(T data, Node<T> next) {
        this.data = data;
        this.next = next;
    }

    public T getData() { return data; }
    // ...
}
```
擦除为：
```Java
public class Node {

    private Comparable data;
    private Node next;

    public Node(Comparable data, Node next) {
        this.data = data;
        this.next = next;
    }

    public Comparable getData() { return data; }
    // ...
}
```
注意，以上这两个都是定义了一个Node泛型类，不能两种同时存在，所以不存在：编译器如何区分这两个都叫Node的类的问题。

而
```Java
ArrayList<? super Number> list1 = new ArrayList<>();
ArrayList<Number> list2 = new ArrayList<>();
```
是在声明类型，不存在导致生成两个Node类的问题，这里的`<? super Number>`和`<Number>`只存在在符号表里，用作类型安全的考量。

2.在必要的地方插入类型转换，见[这个链接](https://www.zhihu.com/question/660964701/answer/3577600043)里的例子：

```Java
public class GenericExample<T> {
    private T value;

    public GenericExample(T value) {
        this.value = value;
    }

    public T getValue() {
        return value;
    }
}

public class Main {
    public static void main(String[] args) {
        GenericExample<String> stringExample = new GenericExample<>("Hello, World!");
        System.out.println(stringExample.getValue());
    }
}
```
处理为：
```Java
public class GenericExample {
    private Object value;

    public GenericExample(Object value) {
        this.value = value;
    }

    public Object getValue() {
        return value;
    }
}

public class Main {
    public static void main(String[] args) {
        GenericExample stringExample = new GenericExample("Hello, World!");
        System.out.println((String)stringExample.getValue());
    }
}
```

### Java伪泛型导致的限制

**禁止泛型数组**

参考<https://www.zhihu.com/question/20928981/answer/39234969>

Java禁止创建泛型数组，例如下面这样
```Java
ArrayList<Integer>[] arr = new ArrayList<>[3];
```
不能编译通过。

Java禁止这样做的原因在于：Java里若A是B的父类，则A[]也被认为是B[]的父类，于是，Object[]数组是任何数组的父类。

于是可以这样做：
```Java
ArrayList<Integer>[] arr = new ArrayList[3];
Object[] objArr = arr;
objArr[0] = new ArrayList<String>();
arr[0].add(1); // 实际应当是ArrayList<String>的，放进去了Integer
System.out.println(arr[0].get(0));
```
这样写是能编译运行的，造成的效果是把一个Integer放进了`ArrayList<String>`，违背了类型安全，但是由于运行时都是Object，这里代码实际也能运行。

应该是为了避免这种迷惑性及可能导致的问题，Java禁止了泛型数组的写法，但是可以用raw type的写法避开，例如`ArrayList<Integer>[] arr = new ArrayList[3];`或者直接`ArrayList[] arr = new ArrayList[3];`，由于历史遗留问题，Java又不能直接禁止raw type的写法。不过用raw type，本质没发生改变。

泛型数组的一个更严重的例子：
```Java
ArrayList[] arr = new ArrayList[3];
Object[] objArr = arr;
objArr[0] = new String("aa");
arr[0].add(1);
System.out.println(arr[0].get(0));
```
这样的代码能编译运行，不过在`objArr[0] = new String("aa");`这行会被运行时的类型检查检查出来往数组里放的元素类型不对。

**对泛型所知的信息有限**

Java里这样写，`t.sayName()`那里会直接报错The method sayName() is undefined for the type T。因为编译器根本不知道T有没有sayName()方法。
```Java
class MyContainer<T> {
    public void call(T t) {
        t.sayName();
    }
}

class Person {
    public void sayName() {
        System.out.println("Tom");
    }
}
```

而C++里这样写：
```cpp
template<typename T>
class MyContainer {
public:
    void call(T t) {
        t.sayName();
    }
};

class Person {
public:
    void sayName() {
        cout << "Tom" << endl;
    }
};

int main(int argc, char const *argv[]) {
    MyContainer<Person> c;
    c.call(Person());
    return 0;
}
```
能正常编译运行，如果Person没定义sayName()函数也能在编译时报错，因为模板根据实际类型过了一遍编译器去生成实际的类型。

**泛型参数限制为对象**

Java的伪泛型没去生成实际的类型，实际用的是同一个，而Java的所有对象名都是一个指针，刚好长度是相同的，所以也不会有什么问题。但是如果泛型参数可以为各种长度的类型，例如float和double，那么用那个泛型参数都处理为Object的类就会有问题（例如一个泛型类有一个float成员或是一个double成员，struct的长度就根本不一样），所以要把泛型参数限制为对象。
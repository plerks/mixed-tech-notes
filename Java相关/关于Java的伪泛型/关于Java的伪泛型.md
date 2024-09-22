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
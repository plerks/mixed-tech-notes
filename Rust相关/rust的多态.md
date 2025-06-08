# 多态
多态是指，对象的实际类型与声明类型不一致，一个对象被声明为一个类型，但是其实际可以是多种实际类型。运行时通过虚表指针与虚函数表，会调用到实际类型的方法。

**多态必须对指针才会发生**，一个子类的指针可以赋值给一个基类的指针，然后通过指针调用方法时通过动态绑定的机制，调用到实际类型对应的方法。

注意必须通过指针调用才会发生动态绑定，以以下c++为例，这样写：
```cpp
#include <bits/stdc++.h>

using namespace std;

struct GeometricObject {
    virtual void printName() {
        cout << "GeometricObject" << endl;
    }
};

struct Circle : GeometricObject {
    int radius;

    virtual void printName() {
        cout << "Circle" << endl;
    }
};

int main(int argc, char const *argv[]) {
    Circle c;
    GeometricObject g = c;
    g.printName(); // 打印 GeometricObject

    Circle* c_ptr = new Circle();
    GeometricObject* g_ptr = c_ptr;
    g_ptr->printName(); // 打印 Circle
    return 0;
}
```
c 和 g 是两个对象，其声明类型一定就是实际类型，不存在什么多态。

而 c_ptr 和 g_ptr 是两个指针，指向同一个对象（这个对象的实际类型为 Circle），动态绑定会使得 g_ptr->printName() 调用到实际类型的 printName() 方法。


# rust的多态
考虑多态的情景：一个对象被声明为一个类型，但是其实际可以是多种实际类型。

rust中struct不能继承，trait可以有继承关系。但是trait不能用来构造对象，也即trait不能作为实际类型，实际类型只能为struct；而声明类型不能为struct，因为rust不可能一个struct B : struct A。

于是：rust的多态只能是：

声明类型为trait指针，实际类型可以是多种struct。

trait指针怎么来？

rust不直接用指针，对应的是引用，那么就是`&dyn Trait`，trait对象的引用。这里必须显式写出这个dyn，rust要求明确写出这是一个trait object。(早期的rust编译器可以省略dyn，现在要求明确写出，这样可以一眼看出在用Trait做类型)

## 方式一：&dyn Trait多态

```Rust
trait GeometricObject {
    fn print_name(&self);
}

struct Circle;
struct Rectangle;

impl GeometricObject for Circle {
    fn print_name(&self) {
        println!("Circle");
    }
}

impl GeometricObject for Rectangle {
    fn print_name(&self) {
        println!("Rectangle");
    }
}

fn print_name(geometric_object: &dyn GeometricObject) {
    geometric_object.print_name();
}

fn main() {
    let circle = Circle;
    let rectangle = Rectangle;

    let ptr: &dyn GeometricObject = &circle;
    ptr.print_name(); // 打印 Circle

    let ptr: &dyn GeometricObject = &rectangle;
    ptr.print_name(); // 打印 Rectangle

    print_name(&circle); // 打印 Circle
    print_name(&rectangle); // 打印 Rectangle
}
```
这里使用了`&dyn Trait`的方法实现多态。与c++不同的是，`&dyn Trait`生成的不是一个普通的指针，是一个“**胖指针**”，其内部不止有data pointer，还有vtable pointer。也就是说，rust的虚表指针不是放在struct头部的，而是放在胖指针里的。这样，rust的struct就是单纯的struct，没有其它成分。

关于胖指针，参考[这个](https://stackoverflow.com/questions/57754901/what-is-a-fat-pointer)。像`&i32`这样的指针只需要8字节记个地址就行了，但是像`&[i32]`和`&dyn Trait`，需要包含切片长度和虚表指针，所以不止8字节。示例：

```Rust
trait GeometricObject {

}

struct Circle;

fn main() {
    dbg!(size_of::<&u32>()); // 8
    dbg!(size_of::<&[u32; 5]>()); // 数组引用，8
    dbg!(size_of::<&[u32]>()); // u32切片，16

    dbg!(size_of::<&Circle>()); // 8
    dbg!(size_of::<&dyn GeometricObject>()); // 16
}
```

无论是普通指针还是胖指针，其实都可以看作一个struct，只是普通指针只有8字节记录地址，胖指针还有其它字段。

`&dyn Trait`这个胖指针相当于是：
```Rust
struct TraitObjectRef {
    data_ptr: *const (), // *const () 代表任意类型的指针，相当于C的void *
    vptr: *const (),
}
```

由于`&dyn Trait`这个胖指针里即有data_ptr，又有vptr，本身又是个trait object(说明想要动态绑定)，rust编译器会对其函数调用产生正确的动态绑定的汇编代码。

## 方式二：将dyn Trait放在容器中多态

```Rust
trait GeometricObject {
    fn print_name(&self);
}

struct Circle;
struct Rectangle;

impl GeometricObject for Circle {
    fn print_name(&self) {
        println!("Circle");
    }
}

impl GeometricObject for Rectangle {
    fn print_name(&self) {
        println!("Rectangle");
    }
}

fn main() {
    let circle = Circle;
    let rectangle = Rectangle;

    let circle: Box<dyn GeometricObject> = Box::new(circle);
    circle.print_name(); // 打印 Circle

    let rectangle: Box<dyn GeometricObject> = Box::new(rectangle);
    rectangle.print_name(); // 打印 Rectangle

    // 如果不显式写Vec<Box<dyn GeometricObject>>，类型会是Vec<Box<Circle>>，装不进Box::new(Rectangle)，编译报错
    let mut vec: Vec<Box<dyn GeometricObject>> = Vec::new(); // 多态集合
    vec.push(Box::new(Circle)); // Circle没有定义字段，这里Circle相当于Circle{}
    vec.push(Box::new(Rectangle));
    vec[0].print_name(); // 打印 Circle
    vec[1].print_name(); // 打印 Rectangle
}
```
调用`vec[0].print_name();`时，Box会通过Deref trait自动解引用。Box的相关代码如下：

```Rust
pub struct Box<
    T: ?Sized,
    #[unstable(feature = "allocator_api", issue = "32838")] A: Allocator = Global,
>(Unique<T>, A);

...

impl<T: ?Sized, A: Allocator> Deref for Box<T, A> {
    type Target = T;

    fn deref(&self) -> &T {
        &**self
    }
}
```
T是`dyn GeometricObject`，vec[0] deref最终得到的结果是`&T`，也就是`&dyn GeometricObject`。和方式一一样了。

但是方式一是直接的借用，生命周期不能超过函数；而方式二通过Box管理生命周期，可以传出去函数，还能实现`Vec<Box<dyn GeometricObject>>`这样的多态集合。

这里有一个细节，`T: ?Sized`表示T这个类型(`dyn GeometricObject`)，可能无法编译期确定大小。Box由于元素分配在堆上而非栈上，编译期无法知道大小也没事。默认情况下，所有泛型参数都是`Sized`，除非显式地声明`?Sized`。`T: ?Sized`可以接收Sized/非Sized类型，但是由于可能非Sized，只能通过`&T`或者`Box<T>`来使用`?Sized`类型。

但是这个`&**self`没看懂，这里参数是`(&self)`，所以`self`的类型为`&Box`，`*self`得到`Box`，然后再次`*Box`又对`Box`调用`deref(&self)`，不是死递归了吗？

# 总结

两种方式其实都落到了`&dyn Trait`这一胖指针类型上，一般应该是用的第二种方式(容器包裹)。注意别写成了let ptr: GeometricObject = circle，Rust不允许用trait表示struct类型，这样不会有作用，而且只知道是Trait类型连内存大小都无法确定。Java才是这样写的，因为Java的对象名全是指针。

# 胖指针，动态大小类型
见[./rust的切片.md](./rust的切片.md)。
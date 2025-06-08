# 切片
slice代表对一段连续内存的引用。

[slice](https://doc.rust-lang.org/std/primitive.slice.html)类型写作`[T]`，但是是个[动态大小类型](#动态大小类型dynamically-sized-types)，不能获得大小：

```Rust
println!("{}", std::mem::size_of::<[i32]>()); // 编译报错，[i32] doesn't have a size known at compile-time
println!("{}", std::mem::size_of::<&[i32]>()); // 胖指针，大小为 16
```
对于`[T]`这个类型，并不真会去创造`[T]`这样一个struct，`[T]`代表已经存在的连续内存空间。

使用`&[T]`(或者`&mut [T]`)获取这段已经存在的连续内存空间的借用。`&[T]`是个struct，可以获得其内存空间大小，其本质是个[胖指针](#胖指针)，包含data_ptr和切片的len。

`&[T]`是对`[T]`的引用，虽然是个特殊的“胖”指针，但是编译器清楚其是个胖指针，清楚其结构，能从其中获得`[T]`的首地址，从而就能调用其方法。[[T]](https://doc.rust-lang.org/std/primitive.slice.html#method.len)是有`len()`方法的：
```Rust
let slice = &[1, 2, 3][0..2]; // slice 的类型为 &[i32]，实际为数组的切片，不过slice不知道原本的对象是什么类型，只知道自己拥有对一片连续i32的借用即可
println!("{}", slice.len()); // 输出 2
```
slice是`[i32]`的胖指针，调用到的`len()`是`[T]`类型的方法，但是切片的长度大小在`&[T]`里，那么`[T]`的`len()`是怎么实现的？

```Rust
// core::slice mod.rs

impl<T> [T] {
    pub const fn len(&self) -> usize {
        ptr::metadata(self)
    }
```
`[T]`的`len()`方法直接读了传进来的`&self`(`&[T]`, 胖指针)里的len信息！`len()`的参数`&self`传进来的`&[T]`是个胖指针，里面有地址和长度！

# 类型如何可切片？Deref trait(Target = [T])
为什么[i32; 5], Vec这样的类型可以切片？

因为其实现了Target为`[T]`的Deref trait。

[Vec文档](https://doc.rust-lang.org/std/vec/struct.Vec.html#impl-Deref-for-Vec%3CT,+A%3E):

```Rust
// alloc::vec mod.rs

impl<T, A: Allocator> ops::Deref for Vec<T, A> {
    type Target = [T];

    #[inline]
    fn deref(&self) -> &[T] {
        self.as_slice()
    }
}

pub const fn as_slice(&self) -> &[T] {
    unsafe { slice::from_raw_parts(self.as_ptr(), self.len) }
}
```
Target的类型是`[T]`，**`deref()`的返回值为`&[T]`**，看`as_slice()`的实现，这时胖指针里记录的len是首地址和全长。像这样的代码：
```Rust
let vec: Vec<i32> = vec![1, 2, 3, 4, 5];
let slice: &[i32] = &vec[1..3]; // Vec的切片
```
实际应该是先用 Vec.deref() 获取到了胖指针`(首地址, 全长)`，然后根据写的切片范围 [1..3]，得到了胖指针`(切片开始地址, 切片长度)`。

可是，Deref trait不是用来自动解引用的吗？怎么被用来支持切片了？

参考[Deref trait](https://doc.rust-lang.org/std/ops/trait.Deref.html)的文档：

> In addition to being used for explicit dereferencing operations with the (unary) * operator in immutable contexts, Deref is also used implicitly by the compiler in many circumstances. This mechanism is called “Deref coercion”. In mutable contexts, DerefMut is used and mutable deref coercion similarly occurs.

里面提到了[Deref coercion](https://doc.rust-lang.org/std/ops/trait.Deref.html#deref-coercion)的概念，我理解，Deref trait不止可以用来解引用，其本质是`deref()`可以获得`&Target`类型的返回值，也就是说，**Deref trait的本质应该是隐式类型转换**。

注意，一个类型不能实现两次Deref trait，即使两次的Target类型不同。

也就是说，一个实现了Deref trait的类型可以隐式转成`&Target`类型，这解释了为什么[Vec的文档](https://doc.rust-lang.org/std/vec/struct.Vec.html#deref-methods-%5BT%5D)里有`Methods from Deref<Target = [T]>`这一段，且把`[T]`的方法全列出来了。因为`Vec`可以隐式转成`[T]`，相当于`Vec`即是`Vec`，也是`[T]`。**Deref trait可以让一个类型同时也(相当于)是另一种类型**。

# 再来理解下 self, &self, &mut self 参数
`&self`其实是个指针，在rust中，指针可能是个胖指针，rust能分清楚`&self`到底是什么样的指针，生成汇编不会有问题。

`&self`和`&mut self`的区别相当于指针是否是const的。

在`f()`中调用`object.g(self)`时，代表object move进了`g()`函数，编译`f()`函数时见到这样的一行调用就知道在这之后object被移动销毁了(object为Copy类型时除外)。

问题是真的把object这个结构体从`f()`的栈帧拷贝到了`g()`的栈帧中了吗？

能不能让self传递的也是个指针？相当于self是个"带着所有权的指针"。由于object的所有权转移进了`g()`，在`g()`内部，有权再次转移object的所有权到其它子函数。如果`g()`函数结束后object仍然活着，则由`g()`来drop(object)。这样，`f()`和`g()`的编译逻辑就都清楚了。

但是这样还不对，因为`g()`可能无法判断到函数末尾时一个object是否还活着，例如`g()`内部有`if`判断。所以，正确的应该是：无论如何，`g()`末尾都插入`drop(self)`，有可能在前面self这个struct已经被移动搬空了，但是无关紧要，struct对应的内存区域还在，调用析构函数即可。

这样看来，参数为`self`时，`g()`的栈帧确实应该给Self这个struct分配了空间，然后把`f()`栈帧中的struct通过移动语义将内容搬到`g()`栈帧中的struct。如果参数为`self`时传进来的是指针，就会发生`g()`去析构`f()`栈帧中的struct，不合理。

# 胖指针
`&[T]`, `&str`, `&dyn SomeTrait`这些引用，本质是个胖指针，除了像普通指针一样有个ptr，还有额外的字段(len, len, vptr)。

# 动态大小类型(Dynamically Sized Types)
[参考1](https://nomicon.purewhite.io/exotic-sizes.html):

> 大多数的时候，我们期望类型在编译时能够有一个静态已知的非零大小，但这并不总是 Rust 的常态。
>
> Rust 支持动态大小的类型（DST）：这些类型没有静态（编译时）已知的大小或者布局。从表面上看这有点离谱：Rust 必须知道一个东西的大小和布局，才能正确地进行处理。从这个角度上看，DST 不是一个普通的类型，因为它们没有编译时静态可知的大小，**它们只能存在于一个指针之后**。任何指向 DST 的指针都会变成一个包含了完善 DST 类型信息的胖指针。
>
> Rust 暴露了两种主要的 DST 类型：
>
> trait objects：dyn MyTrait
>
> slices：[T]、str及其他

[参考2](https://doc.rust-lang.org/reference/dynamically-sized-types.html):

> Most types have a fixed size that is known at compile time and implement the trait Sized. A type with a size that is known only at run-time is called a dynamically sized type (DST) or, informally, an unsized type. Slices and trait objects are two examples of DSTs.
>
> Such types can only be used in certain cases:
>
> * Pointer types to DSTs are sized but have twice the size of pointers to sized types
>
>   * Pointers to slices also store the number of elements of the slice.
>
>   * Pointers to trait objects also store a pointer to a vtable.
>
> * DSTs can be provided as type arguments to generic type parameters having the special ?Sized bound. They can also be used for associated type definitions when the corresponding associated type declaration has a ?Sized bound. By default, any type parameter or associated type has a Sized bound, unless it is relaxed using ?Sized.
>
> * Traits may be implemented for DSTs. Unlike with generic type parameters, Self: ?Sized is the default in trait definitions.
>
> * Structs may contain a DST as the last field; this makes the struct itself a DST.
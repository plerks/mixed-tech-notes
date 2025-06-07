## 闭包的生命周期检查

c++里，类似这样的代码能通过编译：
```cpp
auto make_closure(int x) {
    auto f = [&x](int y) {
        return x + y;
    };
    return f;
}

int main(int argc, char const *argv[]) {
    auto f = make_closure(3);
    cout << f(5) << endl;
}
```
闭包引用捕获了局部变量x，但是返回之后闭包的生命周期比x长，在main里f(5)时，局部变量x已经销毁，但是f这个闭包仍然会去原x所在的地址访问x。这段代码能通过编译，开san运行的话能检查出来访问越界。

但是rust通过生命周期检查，编译期就能检查出来这类问题：
```rust
fn make_closure(x: i32) -> impl Fn(i32) -> i32 {
    |y| x + y
}

fn main() {
    let f = make_closure(5);
    println!("{}", f(3));
}
```
编译时就能检查出：
```Shell
|y| x + y
  |     ^^^ - `x` is borrowed here
  |     |
  |     may outlive borrowed value `x`

并提示：

help: to force the closure to take ownership of `x` (and any other referenced variables), use the `move` keyword
  |
2 |     move |y| x + y // 要写move |y| x + y
  |     ++++
```

## Fn, FnMut, FnOnce的继承关系
[Fn](https://doc.rust-lang.org/std/ops/trait.Fn.html)、[FnMut](https://doc.rust-lang.org/std/ops/trait.FnMut.html)、[FnOnce](https://doc.rust-lang.org/std/ops/trait.FnOnce.html)这三个trait用来描述一个对象的可调用性质。

这3个trait的继承关系为 Fn : FnMut : FnOnce。（FnOnce是最父的类，Fn ⊂ FnMut ⊂ FnOnce）

`Fn trait`说明对象不允许修改捕获。

`FnMut trait`说明对象可以修改捕获，但是也可以选择不进行修改。

`FnOnce trait`说明对象只允许调用一次。

感觉这三个trait的含义与其继承关系是相悖的，Fn可以调用多次，明明不符合FnOnce“**只**允许调用一次”的约束，但是Fn却是FnOnce的子trait。

这三个trait的继承关系的作用在于类型转换，如果一个类型实现了Fn，则其一定也实现了FnMut和FnOnce。可以把`impl Fn`转成`impl FnOnce`，虽然可能实际闭包是可以多次调用的，但是转换之后编译器只知道这个类型impl了FnOnce，于是只允许这个对象调用一次。

闭包定义完成后，编译器会自行分析，得到闭包是哪种 F? trait，并实现相关的trait方法。例如闭包的类型实际 impl Fn，则编译器会把 Fn、FnMut、FnOnce 的方法都实现。

## 关于FnOnce
[官方文档](https://doc.rust-lang.org/std/ops/trait.FnOnce.html)对于FnOnce的描述是：
```
Instances of FnOnce can be called, but might not be callable multiple times. Because of this, if the only thing known about a type is that it implements FnOnce, it can only be called once.
```
注意：**lambda定义处的move前缀仅仅代表强制都使用值捕获，并不一定就是FnOnce，得看是move传值还是copy传值。没有move前缀，编译器会自行决定是传引用，可变引用，还是传值，相当于是种智能捕获(最小权限，引用优先，必要时传值)**。例如，如果被捕获的变量s在闭包函数体里调用了s.into() (参数需要传self)，则s需要采用值捕获。

如果发生了move传值，生成的闭包一定会也只能是FnOnce。而由于自己获得了所有权，自然可以自行选择修改/不修改。

## 闭包的实际类型、impl Trait描述类型、impl Trait类型转换

`let x = 2; let f1 = move |y| x;`，f1这个闭包的实际类型是什么？

**f1 的实际类型是匿名的，只有编译器知道**，源代码中写不出来。但是知道这个匿名类型实现了 Fn trait (从而这个匿名类型也实现了 FnMut 和 FnOnce)。impl Fn 最充分地描述了 f1 的实际类型实现的trait。

`impl Fn(i32) -> i32`描述了这个匿名类型，但是**不要认为 impl Fn(i32) -> i32 是闭包的实际类型**，不能直接写 `let f1: impl Fn(i32) -> i32 = move |y| x;`，会报错。**因为 impl Fn(i32) -> i32 并不是f1的实际类型，f1的实际类型是个编译器才知道的匿名类型**。

```Rust
let x = 2;
let f1 = move |y| x; // x是copy传值进来的，不会消耗x。f1的实际类型是个实现了Fn trait的匿名类型(impl Fn(i32) -> i32)。
println!("{}", f1(3));
println!("{}", f1(3)); // 可以调用

/* 不能直接写 let f1: impl Fn(i32) -> i32 = move |y| x，会报错提示：
`impl Trait` is not allowed in the type of variable bindings。
`impl Trait` is only allowed in arguments and return types of functions and methods
*/

let s = "abcd".to_string();
let f2 = move |y| s; // f2的实际类型是个实现了FnOnce trait的匿名类型(impl FnOnce(i32) -> String)
println!("{}", f2(3));
println!("{}", f2(3)); // 不能调用，提示value used after move
```

再次强调`impl Trait`的写法只是个描述，并不是个实际类型，不能作为类型名。无论是 impl Trait 还是 Trait，**都不是实际struct的类型**，都不能作为变量声明时的类型名，trait只描述行为/增添方法，不能增添属性，**trait不影响struct内存大小且从trait根本不知道struct内存的大小**，`let x: MyTrait`或者`let x: impl MyTrait`的写法都不能通过rust编译器，必须不写出类型，let x: auto，让编译器自己去填这个auto。

虽然`impl Trait`的类型写法不能用于声明变量，但是**可以用在参数和返回值中**，用来代替源代码里写不出来的匿名类型。且可以转换trait的类型：

这样写，f能调用多次：
```Rust
fn make_closure(x: i32) -> impl Fn(i32) -> i32 {
    move |y| x + y // 闭包实际实现了Fn、FnMut、FnOnce trait
}

fn main() {
    let f = make_closure(5);
    println!("{}", f(3));
    println!("{}", f(3));
}
```

但是把make_closure返回值类型声明为FnOnce则不行：
```Rust
fn make_closure(x: i32) -> impl FnOnce(i32) -> i32 {
    move |y| x + y // 闭包实际实现了Fn、FnMut、FnOnce trait
}

fn main() {
    let f = make_closure(5);
    println!("{}", f(3));
    println!("{}", f(3)); // 不能调用
}
```
这里`move |y| x + y`，捕获的x是i32，实现了copy trait，这个闭包实际类型是个`impl Fn(i32) -> i32`的匿名类型。但是返回值声明为只实现了 FnOnce trait (这个trait类型转换是安全的)，所以编译main时不允许调用多次。

这里用`impl FnOnce(i32) -> i32`来表示返回值类型，编译器知道实际的类型是什么，大小为多大，能生成正确的汇编代码吗？

知道，编译器知道匿名类型的大小，能生成正确的汇编代码：

```rust
// main.rs

use std::any::type_name;
use std::mem::size_of_val;

fn make_closure(x: i32) -> impl Fn(i32) -> i32 {
    move |y| x + y
}

fn print_type_and_size<T>(_: &T) {
    println!("Type: {}", type_name::<T>());
    println!("Size: {}", std::mem::size_of::<T>());
}

fn main() {
    let f = make_closure(5);
    // println!("Size of f: {}", size_of_val(&f));
    print_type_and_size(&f);
}
```
`rustc main.rs -o main.exe`之后运行，输出为：
```
Type: main::make_closure::{{closure}}
Size: 4
```
虽然写的是`impl Fn(i32) -> i32`，但是编译器知道具体类型是什么，能生成正确的汇编代码，可以把`impl Fn(i32) -> i32`这个写法看成auto。

这里获取闭包的类型名，调用的是`intrinsics::type_name::<T>()`
```Rust
// intrinsics::type_name::<T>()

#[rustc_nounwind]
#[unstable(feature = "core_intrinsics", issue = "none")]
#[rustc_intrinsic]
pub const fn type_name<T: ?Sized>() -> &'static str;
```
这个函数没有函数体，是因为`#[rustc_intrinsic]`表明这个函数是编译器内建的，编译器知道怎么处理这个函数，编译期就会生成这个闭包的名字（一个全局字符串）。运行时，是没有“类型”的概念的，假设一个库crate有个公开的函数返回一个闭包，且这个库crate被编译为了静态库(不知道rust具体能不能这样，假设能)，然后当前crate直接引用静态库，只要知道闭包struct的大小，并链接上静态库中的函数，应该是可以编译生成正确代码的。即使是闭包，运行时应该也是没有“类型”的概念的，上面能打印出闭包名只是编译期生成了个全局字符串放到了可执行文件中。

## Rust lambda的实现原理
上面的这个代码：
```Rust
fn make_closure(x: i32) -> impl FnOnce(i32) -> i32 {
    move |y| x + y // impl Fn
}

fn main() {
    let f = make_closure(5);
    println!("{}", f(3));
    println!("{}", f(3)); // 不能调用
}
```
这里有个点，第二次调用时报错的描述是在第一次f(3)处: `f moved due to this call`。会去想rust实现FnOnce只能调用一次的方式是：
让闭包的operator()的第一个参数是self(lambda对象)，这样就自然消耗了f，实现让f只能调用一次。但是仅仅这样不行，因为f其实是impl Fn，如果要生成operator()，第一个参数(形参)应当是&self，但是main里生成代码时只知道是FnOnce，会传递self(实参)，函数调用的参数传递就完全错了，类型大小都不一样。

实际rust的闭包可调用，不是通过定义operator()，而是 Fn, FnMut, FnOnce 各有一个调用方法。

结合deepseek的回答和rust官方文档的Fn, FnMut, FnOnce定义。Fn, FnMut, FnOnce各有一个方法call, call_mut, call_once。

[官方文档](https://doc.rust-lang.org/std/ops/trait.Fn.html)的定义：
```Rust
pub trait Fn<Args>: FnMut<Args>
where
    Args: Tuple,
{
    // Required method
    extern "rust-call" fn call(&self, args: Args) -> Self::Output;
}

pub trait FnMut<Args>: FnOnce<Args>
where
    Args: Tuple,
{
    // Required method
    extern "rust-call" fn call_mut(
        &mut self,
        args: Args,
    ) -> Self::Output;
}

pub trait FnOnce<Args>
where
    Args: Tuple,
{
    type Output;

    // Required method
    extern "rust-call" fn call_once(self, args: Args) -> Self::Output;
}
```

例如，一个lambda是impl Fn，那么依据三种trait的包含关系，那么这个lambda就要同时实现call, call_mut和call_once！！！并不是像c++的lambda那样只需实现一个operator()。

例如，如果lambda是impl Fn，则其实现Fn，同时也要实现FnMut和FnOnce，对于FnMut和FnOnce的方法，将其实现桥接到Fn的方法，这样无论以哪种声明类型来调用lambda，结果都会是对的。

deepseek对于桥接的示例：
```Rust
fn create_closure() -> impl FnOnce() {
    let x = 42;
    || println!("{}", x) // impl Fn
}

// 底层生成的匿名结构体：
struct __Closure1 {
    x: i32
}

impl FnOnce<()> for __Closure1 {
    type Output = ();
    fn call_once(self, _: ()) { ... }
}

impl Fn<()> for __Closure1 { // 自动实现 Fn
    fn call(&self, _: ()) { ... }
}

// 伪代码展示转换逻辑
impl FnOnce<()> for __Closure1 {
    fn call_once(self, args: ()) {
        // 对于实现了 Fn 的闭包，这里实际上调用 Fn::call
        <__Closure1 as Fn<()>>::call(&self, args)
    }
}
```

验证代码：
```Rust
#![feature(fn_traits)]
fn main() {
    let mut f = || 3; // f为 impl Fn
    // 手动调用lambda的方法
    println!("{}", Fn::call(&f, ()));
    println!("{}", FnMut::call_mut(&mut f, ()));
    println!("{}", FnOnce::call_once(f, ()));

    let mut x = 1;
    let mut f_mut = || { x += 1; x }; // f_mut为 impl FnMut
    // println!("{}", Fn::call(&f_mut, ())); // FnMut不是Fn类型，这行编译期能检查出来
    println!("{}", FnMut::call_mut(&mut f_mut, ()));
    println!("{}", FnOnce::call_once(f_mut, ()));
}
```

### 为什么 值捕获(有move前缀) + 被捕获的变量非Copy => 必须让这种闭包只能调用一次(是FnOnce类型)
闭包相当于是个实现了operator()的struct，值捕获时，被捕获变量以值的方式成为这个struct的成员。

如果被捕获变量(记为s)没有实现Copy trait，那么，假设第一次调用operator()时函数内部读取s并在最后`return s;`，那么struct里的s成员就被销毁了。第二次调用operator()时就会去读取一个被销毁的变量，不符合rust的语义。所以，必须约束这样的lambda只能调用一次。

如果s实现了Copy，则s不会被销毁，可以多次调用。

（放c++里会怎样？成员s是左值，返回时会被拷贝，所以没有问题，相当于rust里实现了Copy；若强行`return std::move(this->s)`，this->s被搬空，但是c++里不存在销毁变量的概念，所以也只是s被搬空，c++不会约束operator()只能调用一次）

逻辑上相当于这样：
```Rust
struct Lambda {
    s: String
}

impl Lambda {
    fn operator_parentheses(self) -> String {
        return self.s;
    }
}

fn main() {
    let l = Lambda { s: String::from("abcd") };
    l.operator_parentheses();
    l.operator_parentheses(); // 提示 l used here after move
}
```

涉及泛型约束时的例子：
```Rust
fn make_closure1<T>(x: T) -> impl FnOnce() -> T { // T没有Copy约束，返回值类型不能是 impl Fn
    move || x
}

fn make_closure2<T: Copy>(x: T) -> impl Fn() -> T {
    move || x
}
```
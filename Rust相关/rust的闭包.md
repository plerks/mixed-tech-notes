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

## 关于FnOnce
[官方文档](https://doc.rust-lang.org/std/ops/trait.FnOnce.html)对于FnOnce的描述是：
```
Instances of FnOnce can be called, but might not be callable multiple times. Because of this, if the only thing known about a type is that it implements FnOnce, it can only be called once.
```
注意：**lambda定义处的move前缀仅仅代表强制都使用值捕获，并不一定就是FnOnce，得看是move传值还是copy传值。没有move前缀，编译器会自行决定是传引用，可变引用，还是传值，相当于是种智能捕获(最小权限，引用优先，必要时传值)**。例如，如果被捕获的变量s在闭包函数体里调用了s.into() (参数需要传self)，则s需要采用值捕获。

```Rust
let x = 2;
let f1 = move |y| x; // f1的实际类型是 impl Fn(i32) -> i32，因为x是copy传值进来的，不会消耗x
println!("{}", f1(3));
println!("{}", f1(3)); // 可以调用

/* f1的实际类型是 impl Fn(i32) -> i32，但是不能写明f1类型，会提示：
`impl Trait` is not allowed in the type of variable bindings。
`impl Trait` is only allowed in arguments and return types of functions and methods
*/

let s = "abcd".to_string();
let f2 = move |y| s; // f2的实际类型是 impl FnOnce(i32) -> String
println!("{}", f2(3));
println!("{}", f2(3)); // 不能调用，提示value used after move
```

这里也有实际类型和声明类型的概念

这样写，f能调用多次：
```Rust
fn make_closure(x: i32) -> impl Fn(i32) -> i32 {
    move |y| x + y // 实际类型为Fn
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
    move |y| x + y // 实际类型为Fn
}

fn main() {
    let f = make_closure(5);
    println!("{}", f(3));
    println!("{}", f(3)); // 不能调用
}
```
这里`move |y| x + y`，捕获的x是i32，实现了copy trait，这个闭包实际类型是`impl Fn(i32) -> i32`。但是返回值声明为FnOnce，所以编译main时不允许调用多次。

注意`Fn ⊂ FnMut ⊂ FnOnce`，所以虽然实际类型是Fn，但是可以作为FnOnce类型返回。

## Fn, FnMut, FnOnce的包含关系
`Fn ⊂ FnMut ⊂ FnOnce`

Fn: 不允许修改捕获

FnMut: 可以修改捕获，但是也可以选择不进行修改

FnOnce: 直接获得捕获的所有权(move传值)，自己有所有权，自然可以修改/不修改。但是只允许调用一次

## Rust lambda的实现原理
上面的这个代码：
```Rust
fn make_closure(x: i32) -> impl FnOnce(i32) -> i32 {
    move |y| x + y // 实际类型为Fn
}

fn main() {
    let f = make_closure(5);
    println!("{}", f(3));
    println!("{}", f(3)); // 不能调用
}
```
这里有个点，第二次调用时报错的描述是在第一次f(3)处: `f moved due to this call`。会想到rust实现FnOnce只能调用一次的方式是：
让闭包的operator()的第一个参数是self(lambda对象)，这样就自然消耗了f，实现让f只能调用一次。但是仅仅这样不行，因为f的实际类型是Fn，如果要传self，第一个参数(形参)应当是&self，但是main里生成代码时只知道是FnOnce，会传递self(实参)，函数调用的参数传递就完全错了，类型大小都不一样。这样看来，闭包的operator()参数好像又应当不能传递self。但是问题又来了，没有传递self，lambda的函数体内部又如何知道自己这个struct的首地址呢？

问题为：如果有一个函数，其返回一个FnOnce，但是返回值的实际类型是Fn。当我在main函数里使用这个闭包，由于声明类型与实际类型的不同，传递参数时不会错误地传递吗？

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

例如一个lambda的实际类型是Fn，那么依据三种trait的包含关系，那么这个lambda就要同时实现call, call_mut和call_once！！！并不是像c++的lambda那样只需实现一个operator()。

例如，如果lambda的实际类型是Fn，则其实现Fn，同时也要实现FnMut和FnOnce，对于FnMut和FnOnce的方法，将其实现桥接到Fn的方法，这样无论以哪种声明类型来调用lambda，结果都会是对的。

deepseek对于桥接的示例：
```Rust
fn create_closure() -> impl FnOnce() {
    let x = 42;
    || println!("{}", x) // 实际类型是 Fn
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
    let mut f = || 3; // f实际类型为Fn
    // 手动调用lambda的方法
    println!("{}", Fn::call(&f, ()));
    println!("{}", FnMut::call_mut(&mut f, ()));
    println!("{}", FnOnce::call_once(f, ()));

    let mut x = 1;
    let mut f_mut = || { x += 1; x }; // f_mut实际类型为FnMut
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
fn make_closure1<T>(x: T) -> impl FnOnce() -> T { // T没有Copy约束，返回值类型不能是Fn
    move || x
}

fn make_closure2<T: Copy>(x: T) -> impl Fn() -> T {
    move || x
}
```
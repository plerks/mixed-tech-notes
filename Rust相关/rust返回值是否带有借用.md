这样的代码：
```Rust
struct Circle {
}

impl Circle {
    fn f(&mut self, x: i32) -> Result<i32, &str> {
        if x > 0 { return Ok(x); }
        else { return Err("x < 0") };
    }
}

fn main() {
    let mut c: Circle = Circle {};
    let r1: Result<i32, &str> = c.f(1);
    let r2: Result<i32, &str> = c.f(2);
    println!("{:?}", r1);
    println!("{:?}", r2);
}
```
无法通过编译。f的参数有`&mut self`，调`c1.f(1)`时，把c的可变借用传进去了，然后rust编译器会认为`f()`的返回值里(可能)含有对self的借用，这里编译器无法完美判断，只能保守估计，认为是有的。于是r1就持有了c的可变借用，同理，r2也持有c的可变借用，这违背了rust同一时间对一个数据最多只能有一个可变借用的原则，所以会编译报错。（注意，这里至少要用一次r1，也就是至少要println!("{:?}", r1)，不然编译器应该是把let r1 = c.f(1)和let r2 = c.f(2)优化掉了，没有编译报错）

但是，实际以上的f()实现，返回值里是没有对self的借用的，只是编译器无法细致分析，发现返回值有引用的成分，不知道是不是对self的引用，做了保守估计。标明返回值的&str是static的，可以通过编译：
```Rust
struct Circle {
}

impl Circle {
    fn f(&mut self, x: i32) -> Result<i32, &'static str> {
        if x > 0 { return Ok(x); }
        else { return Err("x < 0") };
    }
}

fn main() {
    let mut c = Circle {};
    let r1 = c.f(1);
    let r2 = c.f(2);
    println!("{:?}", r1);
    println!("{:?}", r2);
}
```
如果f()的返回内容是值，也可以通过编译：
```Rust
#[derive(Debug)]
struct Circle {
}

impl Circle {
    fn f(&mut self, x: i32) -> Circle {
        if x > 0 { return Circle {}; }
        else { return Circle {} };
    }
}

fn main() {
    let mut c: Circle = Circle {};
    let r1: Circle = c.f(1);
    let r2: Circle = c.f(2);
    println!("{:?}", r1);
    println!("{:?}", r2);
}
```

## rust编译器如何分析返回值是否会带出对self的借用？
我猜可能是这样（只是纯猜测，还没系统学习rust的相关内容）：

如果返回值是单纯的值，不会带出self；

如果返回值是引用，则检查生命周期，如果生命周期严格大于self的生命周期，则肯定不会带出self；否则做最保守的估计，认为会带出self;

如果返回值是结构体，则分析结构体结构，然后用上面两种情况的原则检查；
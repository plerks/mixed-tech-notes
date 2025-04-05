## 关于rust所有权
这样来理解rust的所有权转移：rust中有两种传值(=)的方式，move传值和copy传值。

1. 如果类型没有实现Copy trait，则传值时使用类似cpp的移动语义，就算是左值也无需std::move，直接就是触发移动。

   不仅仅是像cpp的移动语义那样对象被搬空，而且变量还会直接被rust编译器认为销毁(所有权转移)，不再可用。实现上，当rust编译器编译时发现一个变量发生了move传值后，就记录这个变量已经失效，不过rust可以重新用let声明同名的变量。

2. 如果类型实现了Copy trait，则会发生复制而非移动（copy传值优先，如果实现了Copy的话），所有权不转移，变量不失效。基本数据类型(例如i32)都实现了Copy trait，传值发生复制而非移动，所以基本类型不用考虑所有权转移然后变量失效的问题。

使用&创建引用，称为所有权借用。

rust的函数必须明确写出参数类型和返回值类型。且不像C++那样无论形参是T&还是T，实参都是写t。rust想传引用时实参要写成&t，所以可以一眼看出是传值还是传引用。

## 多重引用
如下代码：
```Rust
enum Progress {
    None,
    Some,
    Complete,
}

fn count_iterator(map: &HashMap<String, Progress>, value: Progress) -> usize {
    map.values().filter(|v: &&Progress| **v == value).count()
}
```
map.values()的迭代器的元素类型是&Progress(值的引用)，filter传递参数给闭包时又会加一层引用，所以lambda参数的类型为`&&Progress`双重引用，得两次解引用。或者.filter(|&&v| v == value)，这样v的类型就变成了Progress。不过rust可以自动推导，实际不用写出来v的类型。

但是.filter后面就算再套.find(|v: &&Progress| v == 2)，这个v不是三重引用，而是迭代器元素引用传递给lambda，所以还是双重引用。

那么，rust会不会有更多重的引用呢?

有，如下代码是可以正常运行的：
```Rust
fn f3(x: &&&i32) {
    println!("{}", ***x); // 这里写x, *x, **x, ***x都能正常运行
}

fn f2(x: &&i32) {
    f3(&x);
}

fn f1(x: &i32) {
    f2(&x);
}

fn main() {
    let x = 1235751;
    f1(&x);
}
```
f1内部调`f2(&x);`时，给f2的参数确实是`&&i32`，同理，f2内部给f3的&x的类型确实是`&&&i32`。

rust如何处理多重引用？

例如，`let r3 = &&&x`，大概是这样实现：
```Rust
let x = 1;
let r3 = &&&x;
转化为：
let x = 1;
let r1 = &x;
let r2 = &r1;
let r3 = &r2;
```
这样r3就是多重引用`&&&x`。

那么，为什么如下的代码能正常运行：
```Rust
fn f3(x: &&i32) {
    println!("{}", x); // 这里写***x就不能编译了，多了一层解引用
}

fn f2(x: &&i32) {
    f3(&x);
}

fn f1(x: &i32) {
    f2(&x);
}

fn main() {
    let x = 1235751;
    f1(&x);
}
```
f2内部调`f3(&x);`时，实参`&x`的类型为`&&&i32`，为什么f3的形参类型`&&i32`可以接收？

rust有Deref trait实现[自动解引用](https://course.rs/advance/smart-pointer/deref.html)的功能。当`&&&i32`传递给`&&i32`时，rust会自动调用一次deref()，把一个`&i32`变成`i32`，这样类型就匹配了，而且还能自动多次deref（多重&传少重&）。

注意：rust可以生成多重引用，也可以通过Deref实现多传少。但是实测不支持少传多（理论上可以通过传值时生成中间引用变量实现，但没价值，Deref才是有用的）：
```Rust
let x = 1;
let y: &&i32 = &&&&&&&x; // Ok
let z: &&&&i32 = &x; // Err
```

所以，看起来如此混乱的引用，实际也是对的：
```Rust
// f2给f3的实参类型为11个&，所以f3的形参这里的&个数要 <= 11，大于11的话会编译报错。
fn f3(x: &&&&&&&&&&&i32) {
    println!("{}", x); // 这里写x，自动多次解引用
}

fn f2(x: &&i32) {
    f3(&&&&&&&&&x); // 生成了多重引用
}

fn f1(x: &i32) {
    f2(&x);
}

fn main() {
    let x = 1235751;
    f1(&x);
}
```

注意，以上容错性这么强是因为i32实现了Deref trait，像上面自定义的Progress类型没有实现Deref trait，就必须手动解引用，写成`map.values().filter(|v| **v == value).count()`；如果写成`map.values().filter(|v| v == value).count()`就会报错"no implementation for &&Progress == Progress"。

## rust的泛型
rust的泛型，必须要用trait指明泛型类型有哪些函数，否则编译器会直接认定类型没有相应函数。而C++的模板默认有任何函数，当发生实际使用时，才去检查对应的模板实例是否真的有相应的函数。

rust需要用trait表明有哪些函数，这一点上像Java，但是肯定不是像Java那样不同泛型实例共用一份实现，因为不像Java泛型只能装对象且对象全是size相等的指针。对不同类型必须有不同的实现才行。

rust不把泛型设计成c++的模板那样，虽然要声明trait类型麻烦，但是泛型的报错会更明确。

对于rust，只要调用时符合泛型入口处定义的trait要求，即可知道一定是合法的；

而对于c++，只有到模板内部实际调用的地方才能发现实际类型没有相应的函数。

当编译好泛型后，如果实际调用不合法，rust只需根据泛型入口处的类型要求不满足即可报错，而C++则需要进入模版内部，在内部的行处报错，因此报错信息一大堆。

两种实现方式的特点是，rust在编译泛型时即可知道泛型代码是否合法，而c++要到编译实际使用了模板的代码时才能知道。

rust显式指明泛型类型的话得用turbofish写法(::<>)，例如`HashMap::<String, u32>`，`HashMap<String, u32>`不行。

## trait的声明与实现，类型的一致性
```Rust
trait AppendBar {
    fn append_bar(self) -> Self;
}

/*
这里trait的声明和实现，self有些区别，self和mut self是匹配的，因为转移了所有权到当前方法中，是否mutable可由当前方法决定。
&self和&mut self不匹配，因为涉及外部变量，在是否能修改外部变量上需要保持一致。

此外，关于self到底怎么传：
如果方法需要消费原值，用 self
如果方法需要修改原值但保留所有权，用 &mut self
如果方法只需读取，用 &self
*/

impl AppendBar for String {
    fn append_bar(mut self) -> Self {
        self.push_str("Bar");
        self
    }
}
```

## 末尾表达式
if，match，函数，以及作用域块{}，当**末尾**是一个表达式时，rust会直接将表达式的值直接作为这个块的返回值。如果什么都没有的话返回值就是`()`。

注意**直接写return**是跨层的，if里直接写return会让函数直接返回，而不是if语句的返回值。

块有值让rust可以写出这样的代码：`let b = if x > 0 { 1 } else { -1 };`

注意，对于函数，可以中途出现表达式，但是**末尾**出现表达式才会成为返回值，中间出现表达式不会让函数提前返回，只是相当于有了个匿名没使用的中间变量：
```Rust
fn main() {
    let mut x = 1;

    match x {
        1 => x += 1, // 1 => { x += 1; }
        _ => ()
    }

    if x == 1 { () } else { () }

    println!("can print");

    // if x == 1 { 1 } else { 1 } // main的返回值要求为()，所以末尾这里这样写就会提示integer和()不匹配了
}
```
而对于if，match和作用域块{}，中途不会出现表达式：
```Rust
fn main() {
    let mut x = 1;

    if x == 1 {
        x // 编译不通过
        x += 1;
    }
    else {
        x // 编译不通过
        x += 2;
    }

    {
        x // 编译不通过
        x += 3;
    }
}
```

## 杂项
rust没有operator++和--，不允许函数重载，也不允许默认参数值，也不允许可变参数个数(宏函数可以实现可变参数个数)。

rust的enum更像union，里面应该是有个隐藏的值，区分当前enum实例是哪种类型，所以就算内容部分相等了，类型不同还是不同。
然后rust enum里面写的全是enum constructor，根据参数把当前enum实例初始化。但是如果是在match中，例如Message::ChangeColor(r, g, b)这样的写法并不是在调用constructor，而是在匹配并解构，根据enum隐藏的type_id(不一定叫这名，只是表达enum要有个隐藏字段区分enum现在具体是什么类型)，去匹配enum实例的类型，并解构出变量。

rust String可以自动转化为&str，但是&str不能自动转化为String(应该是因为&str转String涉及到动态内存分配，所以设计成不隐式转化)。

rust的迭代器，第一次调用next()时返回第一个元素，也就是说最开始相当于在-1的位置，与c++直接解引用不同。
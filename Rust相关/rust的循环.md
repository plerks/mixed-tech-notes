# for i in Range循环，不受内部修改i影响
rust的`for i in Range`循环，在循环内部修改i是不会影响循环的，每次到循环头部，i是重新生成的。

```Rust
for mut i in 0..5 {
    println!("i: {}", i);
    i -= 1;
}
// 打印 0 1 2 3 4
```

`(0..5)`这种写法，实际是生成了个`core::ops::range::Range`对象，里面有start和end Index。[Range](https://doc.rust-lang.org/std/ops/struct.Range.html)实现了[Iterator trait](https://doc.rust-lang.org/std/ops/struct.Range.html#impl-Iterator-for-Range%3CA%3E)，所以可以循环。

上述代码相当于：

```Rust
let mut iter = (0..n).into_iter();
while let Some(i) = iter.next() {
    // loop body
}
```
i是每次重新解构绑定得到的值。
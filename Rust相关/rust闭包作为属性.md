# 自定义比较器
试了下，rust是可以实现闭包作为对象的属性的，但是 std::collections::BinaryHeap 没有定义自定义比较器的构造函数。应该不是语言能力限制，而是为了简洁，想都通过 Ord trait 来进行比较？

```Rust
struct Heap<F: Fn(i32, i32) -> bool> { // F的泛型约束：Fn(i32, i32) -> bool
    comp: F,
    data: Vec<i32>
}

// 忽略这个Heap实现的性能问题，主要是试能不能自定义比较器
impl<F: Fn(i32, i32) -> bool> Heap<F> {
    pub fn push(&mut self, x: i32) {
        self.data.push(x);
    }

    pub fn peek(&self) -> i32 {
        if self.data.is_empty() { return -1; }
        let mut ans = *self.data.first().unwrap();
        for &x in self.data.iter() {
            if (self.comp)(ans, x) {
                ans = x;
            }
        }
        ans
    }
}

fn main() {
    let comparator = |x: i32, y: i32| {
        match x.cmp(&y) {
            std::cmp::Ordering::Less => true,
            _ => false
        }
    };
    // pq 是 Heap<impl Fn(i32, i32) -> bool>
    let mut pq = Heap {comp: comparator, data: Vec::new() };
    pq.push(5);
    pq.push(7);
    pq.push(2);
    pq.push(3);
    println!("{}", pq.peek()); // 7
}
```
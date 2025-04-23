## Drop trait
rust的Drop trait相当于c++的析构函数，不过有个点是对于所有权被移走的对象，rust是不会对其调用drop的
```Rust
struct Circle {
}

impl Drop for Circle {
    fn drop(&mut self) {
        println!("drop for Circle called");
    }
}

fn main() {
    let c1 = Circle {};
    let c2 = c1;
    // 只打印了一次 drop for Circle called
}
```

而c++: 
```cpp
#include <bits/stdc++.h>

using namespace std;

struct Circle {
    Circle() {

    }

    Circle(Circle&& other) {
        cout << ("move for Circle called") << endl;
    }

    ~Circle() {
        cout << ("destructor for Circle called") << endl;
    }
};

Circle f() {
    Circle c;
    return c;
}

int main(int argc, char const *argv[]) {
    Circle c1;
    Circle c2 = std::move(c1);
    // 会打印两次 destructor for Circle called
}
```

虽然c++调了两次析构，但是实际结果和rust是一样的，`Circle c2 = std::move(c1);`之后c1就空了，虽然调了它的析构，但是实际也没释放什么。

对c++而言，上面是对两个对象(其中一个在调用时已被移空)各调用一次析构函数(注意别和delete释放两次搞混了，析构函数会检查自己的资源并释放)；而对rust而言是对所有权调用一次drop()。

**Rust一个类型不能同时实现Copy和Drop**，因为Copy代表是纯栈对象，Drop代表含有动态内存，这二者是矛盾的。

## std::mem::drop
std::mem::drop可以用来手动调用drop()，且不会有释放两次的问题。

std::mem::drop是个参数传了值的空函数：
```Rust
pub fn drop<T>(_x: T) {}
```

原理是：由于参数传了值，所以所有权转移到了drop()函数内部，所有权传进去之后没传出来，drop()执行完毕，_x离开作用域时，就自动触发了T的drop。（如果T实现了Copy trait，手动调drop不产生任何作用）

这样就实现了手动drop且不会有drop两次的问题，drop()只随着所有权的消失被调用了一次。

而c++如果`c.~Circle();`对局部变量手动调析构就会有析构两次的问题，作用域结束会自动调一次析构。rust知道所有权转移了，作用域结束不会自动调drop()。

---

总结一下，c++的析构函数在对象生命周期结束时被调用，看对象；rust的drop()在对象内部的动态资源生命周期结束时被调用，看所有权。
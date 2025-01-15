参考：
* chatgpt的回答

* https://www.cnblogs.com/diegodu/p/9377438.html

* https://arrebol2020.com/posts/cpp-lamda/

* https://stackoverflow.com/questions/53445857/why-is-a-lambdas-call-operator-implicitly-const

## lambda的捕获是如何实现的
lambda捕获是通过匿名类的成员变量和构造函数传参实现的，捕获的变量会成为编译器生成的匿名类的成员变量。

以
```cpp
int a = 1;
auto lambda = [](int x) { return a + x; };
lambda(2);
```
为例

若是值捕获，则类似于：
```cpp
匿名类定义：
class LambdaClass {
    int a;

public:
    LambdaClass(int a) : a(a) {}

    int operator()(int x) const {
        return a + x;
    }
};

使用lambda的地方：
int a = 1;
LambdaClass lambda(a);
lambda(2);
```

若是引用捕获，则类似于：
```cpp
匿名类定义：
class LambdaClass {
    int& a;

public:
    LambdaClass(int& a) : a(a) {}

    int operator()(int x) const {
        return a + x;
    }
};

使用lambda的地方：
int a = 1;
LambdaClass lambda(a);
lambda(2);
```

## lambda引用捕获局部变量时，生命周期的问题
如果lambda生成的函数对象不发生复制，那么即便其以引用方式捕获局部变量，由于局部变量与其同属一个作用域，生命周期相同，所以不会有问题，但若这个函数对象发生了复制，则其生命周期可能延长，则会有ub的问题。

在[LeetCode855. 考场就座](https://leetcode.cn/problems/exam-room/)中写了类似以下的代码：
```cpp
struct Solution {
    int n;
    set<int, function<bool(const int& x, const int& y)>> st;

    Solution(int n) : n(n), st(
        [&](const int& x, const int& y) {
            auto dist = [&](const int& p) { return abs(p - this->n); };
            return dist(x) < dist(y);
        })
    {
        st.insert(2);
        st.insert(6);
    }
};
```
这里set自定义比较器的逻辑需要用到n，而n又要到构造时才能知道，所以需要在初始化列表里写lambda。如果`p - this->n`写成`p - n`，那么会变成引用捕获局部变量n，而lambda对象会被复制到set里，于是set里的comparator访问n的时候会变成通过指针在访问生命周期已经结束的局部变量n，所以要写`this->n`。

LeetCode855那题写成捕获局部变量n然后运行不会直接出问题，要开san才会有报错：`AddressSanitizer: stack-use-after-return on address ...`

## lambda的operator()是const函数吗
默认情况下，值捕获的变量在lambda的函数体内不可修改，lambda的operator()是const函数。使用mutable可以解除这个限制，这样operator()就不是const了。(注意mutable仅影响按值捕获的变量，且不论有没有mutable，引用捕获的变量都可以修改，引用捕获的变量可以修改不影响operator()是const)

能通过引用修改变量的值不影响函数为const，以下能正常运行(const函数对引用类型的成员变量应该是限制其不能指向其它对象，但引用本身就已具备这个特性)：
```cpp
struct Test {
    int& a;

    Test(int x) : a(x) {}

    int func() const { return ++a; }
};

int main(int argc, char const *argv[]) {
    Test t(1);
    cout << t.func() << endl;
    return 0;
}
```
加上mutable后lambda的operator()不再是const函数的例子(以下代码无法通过编译，lambda是const对象，无法调用非const的operator())：
```cpp
int a = 1;
const auto lambda = [=]() mutable { a++; };
lambda();
```
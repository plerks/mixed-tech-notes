## lambda递归
使用C++的lambda实现直接在函数内定义函数的效果，避免平行定义dfs()传参数麻烦(比如dfs要用到外部的一些变量时经常要写一堆传引用的参数)。但是lambda里需要递归调用时不能直接这样写:
```cpp
// 错误写法
auto dfs = [&](int i) {
    if (i <= 0) {
        cout << "dfs" << endl;
        return;
    }
    dfs(i - 1);
};
```
解决办法是用std::function包装一下
```cpp
function<void(int)> dfs = [&](int i) {
    if (i <= 0) {
        cout << "dfs" << endl;
        return;
    }
    dfs(i - 1);
};

dfs(1);
```

关于C++ lambda表达式的类型，[参考](https://stackoverflow.com/questions/7951377/what-is-the-type-of-lambda-when-deduced-with-auto-in-c11):

lambda得到的结果是个函数对象，而其类型，是会新生成个独特的匿名类型(重写了operator())，而如果lambda没有捕获外部变量的话，会处理成函数指针。

由于lambda表达式的类型是由编译器生成的唯一的匿名类型，只有编译器知道，所以用auto来接收lambda表达式时，这时的auto不是简单的"右边是什么类型自己就是什么类型"，不是那种简单的语法糖。

还有个写法是lambda传入自己作为参数，以下`auto& dfs`也可以写万能引用`auto&& dfs`
```cpp
auto dfs = [&](auto& dfs, int i) { // lambda参数使用了auto，需要C++14及以上
    if (i <= 0) {
        cout << "dfs2" << endl;
        return;
    }
    dfs(dfs, i - 1);
};

dfs(dfs, 1);
```
这种写法看起来比较怪，且要多带一个参数。但是这种比用std::function要快，[LeetCode3186. 施咒的最大总伤害](https://leetcode.cn/problems/maximum-total-damage-with-spell-casting/)这道题，使用std::function会超时，用这种lambda传入自己的技巧就不会，但是不知道为什么使用std::function就会导致超时。

## 这种传入自己的写法是怎么回事
首先，为什么这样写不能通过编译：
```cpp
auto dfs = [&](int i) {
    if (i <= 0) {
        cout << "dfs" << endl;
        return;
    }
    dfs(i - 1);
};
```
右边的lambda表达式会生成一个带operator()的匿名类(这里没捕获变量，所以匿名类没有成员)，然后创建对象：
```cpp
class LambdaClass {
    void operator()(int i) {
        if (i <= 0) {
            cout << "dfs" << endl;
            return;
        }
        dfs(i - 1);
    }
};
LambdaClass dfs = LambdaClass();
```
注意，在生成LambdaClass定义的时候，是没有dfs这个符号的，所以这样写会报错。

```cpp
[&](int i) {
    if (i <= 0) {
        cout << "dfs" << endl;
        return;
    }
    dfs(i - 1);
};
```
会报错 'dfs' was not declared in this scope

```cpp
auto dfs = [&](int i) {
    if (i <= 0) {
        cout << "dfs" << endl;
        return;
    }
    dfs(i - 1);
};
```
会报错 use of 'dfs' before deduction of 'auto'

说明等号左边的dfs会先加到符号表里，但是类型是auto还未推导出来，然后处理右边的lambda时，以为lambda里的dfs是想用等号左边未明确的dfs，所以这样报错。而`function<void(int)> dfs = [&](int i) { dfs(...); }`时，dfs的类型是明确的，所以没问题。

```cpp
auto dfs = [&](int i) {
    if (i <= 0) {
        cout << "dfs" << endl;
        return;
    }
    f(i - 1);
};
```
会报错 'f' was not declared in this scope。

所以，lambda里的dfs需要通过参数传进来：`auto dfs = [&](auto& dfs, int i) {...}`，注意右边的dfs是形参，是传进来的，不是直接就是左边的dfs，只是方便起见命名成一样。(理解的话把右边dfs改名为f更好理解)

这样写：
```cpp
[&](auto& dfs, int i) {
    if (i <= 0) {
        cout << "dfs" << endl;
        return;
    }
    dfs(dfs, i - 1);
};
```
发现是能通过编译的，但是这时候怎么知道dfs这个变量是什么样的，怎么知道它能用两个参数来调用，为什么能允许通过编译？

答案是**模板函数**！

参考<https://blog.csdn.net/zwvista/article/details/41144649>：

c++14起，可以在lambda的参数中使用auto，这叫泛型lambda (generic lambda)。lambda参数带auto，则operator()会处理为模板函数。
```cpp
auto dfs = [&](auto& dfs, int i) {
    if (i <= 0) {
        cout << "dfs" << endl;
        return;
    }
    dfs(dfs, i - 1);
};

dfs(dfs, 1);
```
相当于
```cpp
struct LambdaClass {
    template<typename T>
    void operator()(T& f, int i) const {
        if (i <= 0) {
            cout << "dfs" << endl;
            return;
        }
        f(f, i - 1);
    }
};
auto dfs = LambdaClass();

dfs.operator()(dfs, 1);
```

## lambda需要明确指明返回值的一种情况
有时候会写成这样（比如写完才发现要开long long）
```cpp
auto func = [&](int i, int j) {
    if (i <= 0) return i;
    else return (long long)i * j;
};
```
这种情况下会报错：error: inconsistent types 'int' and 'long long int' deduced for lambda return type

两个返回分支一个返回int，一个返回long long，lambda无法推断返回值类型（这时不存在int自动转为long long），所以要明确指明lambda的返回类型：
```cpp
auto func = [&](int i, int j) -> long long {
    if (i <= 0) return i;
    else return (long long)i * j;
};
```

## 其它auto用法
c++14起可以对返回值用auto，c++20起可以对函数参数用auto
```cpp
auto f(auto x) { return x; }

int main(int argc, char const *argv[]) {
    cout << f(2) << endl;
    return 0;
}
```
对返回值用auto应该类似lambda的返回值类型推导，对参数用auto应该也是通过转成模板函数。
## std::unordered_map定义函数对象类型实现自定义hash函数时, operator()需要定义为const函数，为什么？
unordered_map相关的核心代码应该可以简化成这样，这样写:
```cpp
struct pair_hash {
    int hash(const pair<int, int>& p) {
        return p.first + p.second;
    }
};

class MyUnorderedMap {
public:
    pair_hash hasher;

    int hash_code(const pair<int, int>& key) const {
        return hasher.hash(key);
    }
};
```
会在`return hasher.hash(key);`这里报错:
```shell
error: passing 'const pair_hash' as 'this' argument discards qualifiers [-fpermissive]
```

为什么？

以上代码中，hash_code()函数被定义为const函数 (const函数指成员函数在执行时不会修改类的成员变量)，由于计算hash_code时不应修改当前类 (MyUnorderedMap) 的任何成员变量，所以将hash_code()函数定义为const函数是合理的。

(但感觉不把其定义为const函数也没什么，可能是为了防止hasher两次对同样的输入产生不一样的hash结果？但是实际这样又不能避免，比如将一个全局变量N引入到计算哈希的过程中，并每次N++，这样hasher是不是const都拦不住。另外几个stl数据结构在自定义比较、判等时函数后面就不加const也行)

而**在const函数内部，会隐式地将当前对象的所有成员视为const，即使这些成员在定义时没有用const修饰**。所以才会有：hasher定义时并没有定义为const，但是报错却提示hasher类型为`const pair_hash`，可能const函数禁止修改成员变量就是通过在其内部将所有成员变量视为const实现的。

hasher被视为了const，由于const对象只能调用const类型的成员函数，不能调用非const类型的成员函数，所以`hasher.hash(key);`会报错，需要将hash()定义为const函数：

```cpp
struct pair_hash {
    int hash(const pair<int, int>& p) const {
        return p.first + p.second;
    }
};

class MyUnorderedMap {
public:
    pair_hash hasher;

    int hash_code(const pair<int, int>& key) const {
        return hasher.hash(key);
    }
};
```

### 报错说的 passing 'const pair_hash' as 'this' argument discards qualifiers 具体是怎么回事
首先，实例函数在调用时，会将调用对象的指针隐式地作为第一个参数传递，变为this。

然后，参考[这个](https://blog.csdn.net/anlian523/article/details/95797052)：

* 若实例函数为非const函数，形参this的类型会是`pair_hash * const` (末尾的const指明this是const，即不能改让this指针变量指向别的)

* 若实例函数为const函数，形参this的类型会是`const pair_hash * const`

上面实参`&hasher`的类型是`const pair_hash *`，若hash()不是const函数，传递参数时会出现`const pair_hash *`赋给`pair_hash * const`，丢掉了前面的const限定，因此是非法的。(丢掉const限定非法，多加上合法)

## 实际std::unordered_map的报错
g++版本为`g++.exe (x86_64-win32-seh-rev0, Built by MinGW-Builds project) 14.2.0`，对以下代码：
```cpp
struct pair_hash {
    int operator()(const pair<int, int>& p) {
        return p.first + p.second;
    }
};

int main(int argc, char const *argv[]) {
    unordered_map<pair<int, int>, string, pair_hash> mp;
    mp[{0, 0}] = "Tom";
    return 0;
}
```
会报错，正确写法是operator()末尾要加const，这里主要来看编译报错怎么读。

关键代码 (hashtable_policy.h)：
```cpp
typedef _Hash    hasher;

hasher
hash_function() const
{ return _M_hash(); }

...

__hash_code
_M_hash_code(const _Key& __k) const
{
static_assert(__is_invocable<const _Hash&, const _Key&>{},
"hash function must be invocable with an argument of key type");
return _M_hash()(__k);
}
```

两条关键的报错信息：

```shell
error: static assertion failed: hash function must be invocable with an argument of key type
 1333 |         static_assert(__is_invocable<const _Hash&, const _Key&>{},
      |                       ^~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
```
这里在检查可调用的问题，`_Hash`是可调用对象的类型(类似于上面的pair_hash)，`_Key`是参数的类型，这里检查的应该是对`const _Hash`和`const _Key`能否这样调用：`_Hash()(__Key())`，`_Hash()`得到了函数对象，`__Key()`得到了具体的key。也就是说，const pair_hash，能否以const key为参数调用operator()，上面已经解释了，const对象只能调用const函数，所以operator()最后不写const会有报错。

```shell
error: no match for call to '(const pair_hash) (const std::pair<int, int>&)'
 1335 |         return _M_hash()(__k);
      |                ~~~~~~~~~^~~~~
```
一样的原因。

### 补充下关于const的重载的问题
```cpp
struct Test {
    string f() const { return "const func"; }

    string f() { return "non-const func"; }

    string call() const { return f(); }

    string call() { return f(); }
};

int main(int argc, char const *argv[]) {
    Test t;
    const Test tc;
    cout << t.f() << endl;
    cout << tc.f() << endl;
    cout << t.call() << endl;
    cout << tc.call() << endl;
    return 0;
}
```
输出为：
```
non-const func
const func
non-const func
const func
```

参考[C++ 加const能不能构成重载的几种情况](https://blog.csdn.net/qq_38408573/article/details/116061377)：

函数末尾的const是能构成重载的，但返回值的const不能。

函数是否为const函数与其是否返回const无关，上面也可以写成`const string f() const`，但不能 `string f() const`和`const string f() const`都定义。

`call()`中，如何知道调哪个`f()`的？
看this，如果`(*this)`是const，只能调const函数，如果`(*this)`非const，先找非const函数，再找const函数。
## make_pair的泛型和实参，类型不是完全一致的
这样写：
```cpp
int main() {
    int a = 1;
    make_pair<int, int>(a, 1);
}
```
make_pair()这里是会报错no matching function的，当make_pair()的实参为左值时，泛型的推断会推断为左值引用。

make_pair源码：
```cpp
#if __cplusplus >= 201103L
  template<typename _T1, typename _T2>
    constexpr pair<typename __decay_and_strip<_T1>::__type,
                   typename __decay_and_strip<_T2>::__type>
    make_pair(_T1&& __x, _T2&& __y)
    {
      typedef typename __decay_and_strip<_T1>::__type __ds_type1;
      typedef typename __decay_and_strip<_T2>::__type __ds_type2;
      typedef pair<__ds_type1, __ds_type2> 	      __pair_type;
      return __pair_type(std::forward<_T1>(__x), std::forward<_T2>(__y));
    }
```
上面的代码第一个参数传入的是左值，则万能引用中_T1推导为`int&`，所以泛型应该是`int&`：
```cpp
int main() {
    int a = 1;
    make_pair<int&, int>(a, 1);
}
```
虽然这里make_pair泛型的类型写的是`int &`，不过最终得到的pair是`pair<int, int>`，不会有问题。

所以一般不需要在调用make_pair时写泛型类型，直接`make_pair(a, 1)`就行了，make_pair不需要显式指定泛型类型，可以由实参推导出来。

存在不能推导出来，必须显式指定泛型类型的情况，例如：
```cpp
template<typename T>
void m(T t, T v) {
}

int main() {
    m(1, 1.0); // 编译报错，改成m<double>(1, 1.0)或m<int>(1, 1.0)都行，c++ double可以隐式转int
}
```
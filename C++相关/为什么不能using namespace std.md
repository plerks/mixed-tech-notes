参考：
* <https://www.zhihu.com/question/26911239>
* <https://blog.csdn.net/KingCat666/article/details/44099317>

尽量避免使用using namespace name，因为这会把name(以下以std为例)命名空间下的所有东西引入进来(应该是引入进符号表)，容易占用符号名字造成命名冲突。

如果只是在.cpp里使用了还好，别人不会引入.cpp文件，最多只是造成这个.cpp文件中不能使用很多std中已经用到的命名（如果项目小可能实际还不会发生命名冲突），编译这个.cpp生成.o文件后就不再有任何影响。

但是如果在头文件里using namespace std，这个头文件不知道会被引入到多大范围，可能别人直接或间接地引入了这个头文件，直接导致很多std下已使用的命名别人不能用，得换个名字。头文件中引入应该直接在用到的地方加上命名空间(例如std::cout)，不污染命名。使用using std::cout都会导致cout这个命名被占用。

自己的.cpp代码如果要减少命名冲突，做法应该是直接在用到的地方加上命名空间从而不用using(例如std::cout << 1 << std::endl;)或者using std::cout精确引入。

此外，参考[这个](https://blog.csdn.net/KingCat666/article/details/44099317):

"当我们用using引入一个命名空间的时候，如果之前有引用过别的命名空间（或者同一个命名空间），则不会覆盖掉对之前的引入，而是对之前引入内容的补充。"

例如，这个代码：
```C++
#include <iostream>

using namespace std;

namespace my {
    int a = 1;
}

namespace my2 {
    int a = 1;
}

using namespace my;

using namespace my2;

int main() {
    // 报错"a" is ambiguous
    cout << a << endl;
}
```
就会报错"a" is ambiguous。得cout << my::a << endl;
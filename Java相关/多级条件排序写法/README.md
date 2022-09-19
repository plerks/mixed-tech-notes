**以下用java命令直接运行单个源代码文件需要java版本11及以上(例如java 17)**

运行`java MultipleConditionSortDemo.java`即可

此外，可以指定java运行非.java结尾的源代码文件，不过去掉文件.java后缀情况下需要指定--source参数才能运行，见[Running Single-file Programs without Compiling in Java 11](https://www.infoq.com/articles/single-file-execution-java11/)。
这里可以把MultipleConditionSortDemo.java改成MultipleConditionSortDemo，然后`java --source 17 MultipleConditionSortDemo`运行(17可以改成11等较小数字)

使用java命令直接运行单个源代码文件是从java 11开始的功能，`java --help`列出的[四种launch方式](https://docs.oracle.com/en/java/javase/17/docs/specs/man/java.html#synopsis)的一种。

不过去掉文件.java后缀后，直接`javac MultipleConditionSortDemo`不行了，加上--source也不行，不知道有没有办法让javac编译非.java后缀文件，看[官方文档](https://docs.oracle.com/en/java/javase/17/docs/specs/man/javac.html)的语气:
> Source files must have a file name extension of .java

可能不行。

如果用javac和java来运行的话，先：

`javac MultipleConditionSortDemo.java`

然后到"Java相关"的父目录下：

`java Java相关.多级条件排序写法.MultipleConditionSortDemo`(得在全限定类名开始的目录下用全限定类名运行，实际包名一般不要带中文，只是这里发现java包名包含中文居然不出问题)


对于多条件的排序，代码中例子的规则是：
```
折扣最高的优先
若折扣相同，截至日期更近的优先
若截至日期也相同，id小的优先(不同promotion id保证不相同)
```
Comparator的写法是：
```java
if (x.discount != y.discount) {
    return x.discount > y.discount ? -1 : 1; // 这里也可以写成y.discount - x.discount
}
if (x.endDate != y.endDate) {
    return x.endDate < y.endDate ? -1 : 1;
}
if (x.id != y.id) {
    return x.id < y.id ? -1 : 1;
}
return 0;
```
不要单纯按描述写成：
```java
if (x.discount > y.discount) {
    return -1;
}
if (x.endDate < y.endDate) {
    return -1;
}
if (x.id < y.id) {
    return -1;
}
return 0;
```
这样写是错的
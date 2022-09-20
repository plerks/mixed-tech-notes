VSCode运行maven项目的时候terminal显示运行的命令是：

`
/usr/bin/env /home/gxy/jdk-17.0.4.1/bin/java -XX:+ShowCodeDetailsInExceptionMessages @/tmp/cp_64fyy3cjeetrlewvust54gmr3.argfile com.example.demo.DemoApplication
`

这个`/usr/bin/env java`和直接`java`不知道有什么区别，先不管。

这个@file的写法叫[Command-line argument files](https://docs.oracle.com/en/java/javase/17/docs/specs/man/java.html#using-source-file-mode-to-launch-single-file-source-code-programs)。@后面跟了文件路径，java会把这个路径的文件的内容作为参数。查看`/tmp/cp_64fyy3cjeetrlewvust54gmr3.argfile`文件的内容是`-cp ...`，classpath参数太长了，所以VSCode生成了个临时文件来运行java。

此外，javac也可以用这个[@file写法](https://docs.oracle.com/en/java/javase/17/docs/specs/man/javac.html#examples-of-using-javac-filename)。
### windows下，Java用ProcessBuilder调用外部程序，出现找不到程序的问题。

例如：

```java
String[] cmd = new String[]{"npm", "list", "--json", "-a"};
ProcessBuilder pb = new ProcessBuilder(cmd);
```

然后pb.start()之后报错: java.io.IOException: Cannot run program "npm" (in directory "E:\testNpm"): CreateProcess error=2, 系统找不到指定的文件。

但是直接命令行`npm list --json -a`可以运行。

调python就没问题

```java
String[] cmd = new String[]{"python", "--version"};
ProcessBuilder pb = new ProcessBuilder(cmd);
```

命令行`where npm`结果为：

```
E:\Node.js\npm
E:\Node.js\npm.cmd
C:\Users\gxy\AppData\Roaming\npm\npm
C:\Users\gxy\AppData\Roaming\npm\npm.cmd
```

命令行`where python`结果为：

```
E:\python\python.exe
C:\Users\gxy\AppData\Local\Microsoft\WindowsApps\python.exe
```

估计不写后缀默认是.exe，上面调npm的有两个解决办法

* 改成`String[] cmd = new String[]{"npm.cmd", "list", "--json", "-a"};`（或者写npm程序的绝对路径)
* 改成`String[] cmd = new String[]{"cmd.exe", "npm", "list", "--json", "-a"};`

Linux下没测试，估计没这个问题。


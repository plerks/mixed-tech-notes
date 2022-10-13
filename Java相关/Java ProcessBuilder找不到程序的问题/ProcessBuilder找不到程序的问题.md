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

Ubuntu里试了下也有找不到程序的问题，试了下用ProcessBuilder运行`/bin/bash -c "echo $PATH"`，环境变量里没有~/.bashrc里export的环境变量，只有基础的，所以找不到程序。写程序绝对路径能解决，或者通过ProcessBuilder的environment()方法，设置运行的环境变量。ProcessBuilder运行时应该是没有加载~/.bashrc。参考:
* <https://blog.csdn.net/weixin_44648216/article/details/104056712>
* <https://www.maoyingdong.com/what_is_a_interactive_shell/>
* <https://www.maoyingdong.com/linux_bash_environment_file/>

如果是非交互式非登录式的shell，不会加载~/.bashrc。

判断是否是登录式和交互式终端的方法参考:
* <http://c.biancheng.net/view/3045.html>
* <https://blog.csdn.net/weixin_44648216/article/details/104056712>

判断登录式的两种方法:
* 执行`shopt login_shell`，值为on表示为登录式，off为非登录式。
* 查看`$0`的值，登录式shell返回-bash，而非登录式shell返回bash。

判断交互式的两种方法:
1. 查看`$-`的值，如果值中包含字母i则为交互式。
2. 查看`$PS1`的值，如果非空，则为交互式，否则为非交互式。

通过Ubuntu桌面环境打开的终端是交互式非登录的。此外，参考[这个链接](https://serverfault.com/questions/409994/bashrc-shopt-not-found)，shopt是bash内置的，`whereis shopt`找不到。

用ProcessBuilder运行`bash -c "echo $-"`和`bash -c "shopt login_shell"`，结果是非交互式非登录的，所以应该是没加载~/.bashrc。
### git中文乱码问题解决

---

### linux下

参考链接：

https://www.jianshu.com/p/fc8162ed1e3d

操作方式：

1.运行:

```
$ git config --global core.quotepath false          # 显示 status 编码
$ git config --global gui.encoding utf-8            # 图形界面编码
$ git config --global i18n.commit.encoding utf-8    # 提交信息编码
$ git config --global i18n.logoutputencoding utf-8  # 输出 log 编码
```

2.设置LESSCHARSET环境变量:

`vim ~/.bashrc`，添加export LESSCHARSET=utf-8

这一条按参考链接的说法是因为 git log 默认使用 less 分页，所以需要 bash 对 less 命令进行 utf-8 编码

---


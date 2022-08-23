`vim ~/.bashrc`更改PS1的值之后`source ~/.bashrc`即可，

几个可用的：

* PS1='${debian_chroot:+($debian_chroot)}\[\033[01;32m\]\u@\h\[\033[00m\]:\[\033[01;34m\]\w\[\033[00m\]\n\$ '
* PS1="\[\e[1;36m\]\u@\h\[\033[00m\]:\[\033[01;34m\]\w\[\033[00m\]\\$ " (本地docker run的时候为了让docker里的命令提示符有颜色好区分用过的)

参考链接：

* https://blog.csdn.net/lingeio/article/details/94559587
* https://blog.csdn.net/weixin_41831919/article/details/108631989
* https://blog.csdn.net/u014470361/article/details/81512330
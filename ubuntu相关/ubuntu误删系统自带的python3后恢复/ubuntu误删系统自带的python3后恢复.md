ubuntu版本为22.04

ubuntu自带的python似乎不能随便删，好像ubuntu系统就大量用了python，运行`sudo apt-get remove python3`会连带删很多东西，看输出很多是系统的东西(gnome等等)，所以强制中断了。然后直接无法打开新的终端了，重启进ubuntu之后不会进图形化界面，但是会有一个原生的终端界面，不知道这个原生的终端是什么。

参考[这个链接](https://blog.csdn.net/Knight_vae/article/details/102052256)，需要：
```
sudo dpkg --configure -a
sudo apt install -f
sudo apt-get install ubuntu-minimal ubuntu-standard ubuntu-desktop
```
安装好之后，桌面环境就能恢复了。

此外默认的输入法也出不来了，点设置 -> 区域与语言 -> 管理已安装的语言，直接会提示有包缺失，点确认之后会自动安装，安装好后输入法就会恢复。
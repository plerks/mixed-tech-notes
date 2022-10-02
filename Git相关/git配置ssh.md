windows下的git，进行push之类的操作的时候不需要每次都输账户名和access token(github现在是用access token，gitee是要输密码)，一般第一次push之类的时候会弹出来认证，后面就不需要了。但是ubuntu下的git每次都需要输用户名和access token。

为了在ubuntu下不用每次都输用户名和access token，需要配置ssh。

参考:
* https://segmentfault.com/a/1190000002645623
* https://blog.csdn.net/CityzenOldwang/article/details/77097661
* https://wangdoc.com/ssh/key.html
* https://www.cnblogs.com/f-ck-need-u/p/10484531.html

首先

`ssh-keygen -t rsa -C "邮箱地址"`

默认一路enter的话会在~/.ssh下生成id_rsa和id_rsa.pub。然后在github的Settings -> SSH and GPG keys里添加id_rsa.pub。

本地运行`ssh-add ~/.ssh/id_rsa`，将私钥交给ssh-agent保管，其他程序需要身份验证的时候可以将验证申请交给ssh-agent来完成整个认证过程。

可以用`ssh-add -l`查看ssh-agent中的私钥，`ssh-add -L`查看ssh-agent中的公钥，`ssh-add -d ~/test/id_rsa`删除(如果add之后删除这里这个~/test/id_rsa文件，ssh-add -l里密钥还在，但是ssh-add -d删除会报错，但是又没找到除了`ssh-add -D`外的删除方式)

用`ssh -T git@github.com`测试，提示:Hi `username`! You've successfully authenticated, but GitHub does not provide shell access.就可以了。然后用ssh开头的仓库地址作为remote地址就不需要每次都输用户名和access token了。
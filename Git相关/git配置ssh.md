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

### 可能出现的问题

如果配置好ssh之后发现无法连接到github，例如提示:
```
ssh: connect to host github.com port 22: Connection timed out
fatal: Could not read from remote repository.
```

参考:

* https://blog.csdn.net/KimBing/article/details/135666807

* https://docs.github.com/en/authentication/troubleshooting-ssh/using-ssh-over-the-https-port

```
Sometimes, firewalls refuse to allow SSH connections entirely. If using HTTPS cloning with credential caching is not an option, you can attempt to clone using an SSH connection made over the HTTPS port.
```

也就是说如果22号端口连不上，github支持把https的443端口拿来连ssh，不过: `The hostname for port 443 is ssh.github.com, not github.com`。

使用方式例如:

`ssh -T -p 443 git@ssh.github.com`

`git clone ssh://git@ssh.github.com:443/YOUR-USERNAME/YOUR-REPOSITORY.git`

或者直接改ssh设置，就不用每次指明端口了，在`~/.ssh/config`中添加以下配置:
```
Host github.com
    Hostname ssh.github.com
    Port 443
    User git
```
即可。

如果连接还是有问题，若已在桌面环境和~/.bashrc中设置好了代理环境变量，则尝试在git中也设置:
```bash
git config --global http.proxy http://127.0.0.1:7890
git config --global https.proxy http://127.0.0.1:7890
```
git可能不一定选用了环境变量中的代理地址设置，不过一般来说web client应用是会检测并使用代理环境变量的。
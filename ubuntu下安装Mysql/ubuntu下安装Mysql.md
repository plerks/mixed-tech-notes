### ubuntu下安装Mysql

参考https://www.cnblogs.com/2020javamianshibaodian/p/12920243.html

运行`sudo apt-get install mysql-server`，mysql-client将自动作为依赖项被安装。

然后`sudo mysql -u root`即可进入mysql命令行。

在MySQL 8.0上，默认情况下，root用户通过auth_socket插件进行身份验证。该auth_socket插件对localhost通过Unix套接字文件对进行连接的用户进行身份验证。这意味着不能通过提供密码来以root用户身份进行身份验证(Navicat也没法用root的账户密码连接数据库，不过ssh到服务器再使用mysql命令登录root用户还是可以的)。这个auth_socket插件的意思似乎是通过本地的Unix套接字文件来识别root用户，完全不管密码，我测试的结果`sudo mysql -u root -p错误密码`也可登录

要让Navicat能用账户密码连接数据库，有两种方法：

* 第一个是将身份验证方法从更改`auth_socket`为`mysql_native_password`。您可以通过运行以下命令来做到这一点：

``` 
mysql > ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY 'very_strong_password';
mysql > FLUSH PRIVILEGES;
```

* 推荐的第二个选项是创建一个新的专用管理用户，该用户可以访问所有数据库(我感觉应该用这个)：

```
mysql > CREATE USER 'gxy'@'localhost' IDENTIFIED BY 'very_strong_password';
mysql > GRANT ALL PRIVILEGES ON *.* TO 'gxy'@'localhost'
```

还有一个问题是如果要远程访问mysql，需要开启对应权限，例如：

```
mysql > GRANT ALL PRIVILEGES ON *.* TO 'gxy'@'%'IDENTIFIED BY '远程访问密码' WITH GRANT OPTION;
```

其中 ‘root’@’%’ 中的root表示远程访问数据库的账户，%表示所有IP均可访问，'远程访问密码'表示远程访问mysql时需要的密码，此时就完成了mysql开启远程访问的操作。


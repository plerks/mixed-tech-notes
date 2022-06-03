参考链接：

https://developer.aliyun.com/article/728115

https://www.jianshu.com/p/80ad7ff37744

https://z.itpub.net/article/detail/4A6E5050C092D3D3FFB12B69A2A39547

https://cloud.tencent.com/developer/article/1796826

集群中的一组pod为另一组pod提供服务，pod如何访问这个service?

- 首先我们可以通过 service 的虚拟 IP 去访问，比如说刚创建的 my-service 这个服务，通过 kubectl get svc 或者 kubectl discribe service 都可以看到它的虚拟 IP 地址是 172.29.3.27，端口是 80，然后就可以通过这个虚拟 IP 及端口在 pod 里面直接访问到这个 service 的地址。
- 第二种方式直接访问服务名，依靠 DNS 解析，就是同一个 namespace 里 pod 可以直接通过 service 的名字去访问到刚才所声明的这个 service。不同的 namespace 里面，我们可以通过 service 名字加“.”，然后加 service 所在的哪个 namespace 去访问这个 service，例如我们直接用 curl 去访问，就是 my-service:80 就可以访问到这个 service。
- 第三种是通过环境变量访问，在同一个 namespace 里的 pod 启动时，K8s 会把 service 的一些 IP 地址、端口，以及一些简单的配置，通过环境变量的方式放到 K8s 的 pod 里面。在 K8s pod 的容器启动之后，通过读取系统的环境变量比读取到 namespace 里面其他 service 配置的一个地址，或者是它的端口号等等。比如在集群的某一个 pod 里面，可以直接通过 curl $ 取到一个环境变量的值，比如取到 MY_SERVICE_SERVICE_HOST 就是它的一个 IP 地址，MY_SERVICE 就是刚才我们声明的 MY_SERVICE，SERVICE_PORT 就是它的端口号，这样也可以请求到集群里面的 MY_SERVICE 这个 service。

对第二种方式，需要安装CoreDNS插件，插件的原理是配置pod的DNS解析，Linux 服务器中 DNS 解析配置位于/etc/resolv.conf，k8s配置了这个文件，从而访问时直接写service名当作域名就可以访问到对应的服务(访问不同命名空间的service要加上"."+对应namespace)。
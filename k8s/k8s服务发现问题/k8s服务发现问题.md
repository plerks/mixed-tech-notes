参考链接：

https://developer.aliyun.com/article/728115

https://www.jianshu.com/p/80ad7ff37744

https://z.itpub.net/article/detail/4A6E5050C092D3D3FFB12B69A2A39547

https://cloud.tencent.com/developer/article/1796826

集群中的一组pod为另一组pod提供服务，pod如何访问这个service?

手动操作的情况下可以通过service的虚拟IP去访问，通过`kubectl get svc xxx`或者`kubectl discribe service xxx`可以看到某个service的虚拟ip和端口，然后就可以通过这个虚拟ip及端口在pod里面直接访问到这个service。但是这需要手动获取service的虚拟ip，只有手动操作的时候有用。

基本的服务发现模式有通过环境变量和DNS两种。

[第一种方式](https://kubernetes.io/zh-cn/docs/concepts/services-networking/service/#environment-variables)是通过环境变量，一个pod启动时，k8s会把同一个namespace下的service的ip地址、端口等信息，通过环境变量的方式放到pod里面。

例如，一个名称为redis-master的service暴露了TCP端口6379，同时给它分配了Cluster IP地址10.0.0.11，这个service会生成如下环境变量：
```
REDIS_MASTER_SERVICE_HOST=10.0.0.11
REDIS_MASTER_SERVICE_PORT=6379
REDIS_MASTER_PORT=tcp://10.0.0.11:6379
REDIS_MASTER_PORT_6379_TCP=tcp://10.0.0.11:6379
REDIS_MASTER_PORT_6379_TCP_PROTO=tcp
REDIS_MASTER_PORT_6379_TCP_PORT=6379
REDIS_MASTER_PORT_6379_TCP_ADDR=10.0.0.11
```
使用这种方式对创建顺序有要求，必须在client pod出现之前创建service。否则，这些client pod将不会补充其环境变量。也不实用。

[第二种方式](https://kubernetes.io/zh-cn/docs/concepts/services-networking/service/#dns)直接访问服务名，依靠DNS解析。几乎总是应该选择这种方式。

例如，my-ns命名空间中有一个名为my-service的服务，则my-ns命名空间中的pod可以通过`my-ns`域名来访问服务(`my-service.my-ns`也可以)。

其他命名空间中的pod则使用`my-service.my-ns`域名来访问服务。

对第二种方式，需要安装CoreDNS插件，插件的原理是配置pod的DNS解析，Linux 服务器中 DNS 解析配置位于/etc/resolv.conf，k8s配置了这个文件，从而访问时直接写service名当作域名就可以访问到对应的服务(访问不同命名空间的service要加上"."+对应namespace)。

### k8s非内网节点加入集群
k8s的集群是连在一个内网内的，节点有自己的内网ip地址(应该是对应一个虚拟网卡)。如果有非同一子网的节点要加入集群，需要配置iptables规则进行转化，见[k8s非内网节点加入集群](https://blog.csdn.net/nswdiphone6/article/details/120067820)。

### service是如何实现抽象出集群内的一个服务的
k8s的域名解析能把service名字作为域名，解析出service的ip(通过改`/etc/resolv.conf`)。ingress配置转发规则的时候也是指定转发到的service就行。那么到了service的ip后又如何到实际的pod？

service本身应该是没有实体的，只体现在配置规则里。

k8s的service有ClusterIP,NodePort,LoadBalancer,ExternalName几种方式，ExternalName没接触过。不过对于其它三种service的创建，kube-proxy(kube-proxy是集群中运行在每个节点上的网络代理，维护节点上的网络规则)应该都会在每个节点上应用转发规则。kube-proxy运行模式不止一种，有iptables和ipvs等，用iptables来理解ClusterIP类型的service的话，应该是这样：每当一个新的ClusterIP service创建，k8s的每个节点能知道这个service的创建，并知道这个service选中的pod(k8s集群状态数据应该是存在etcd里)。然后每个节点更新iptables规则，规则应该是这样：对某个service的cluster ip:service port的访问，操作系统会根据iptables里的规则，先转成cluster ip:service targetPort，然后将对应的数据包(这里下沉到了ip层)按轮询方式转发到这个service选中的pod所在的虚拟网卡(这里会跨机器)，然后再到这个虚拟网卡对应的网络命名空间中对应的进程(到了pod里面对应的containerPort)。这个虚拟网卡应该和docker0那个网卡类似，会提供NAT功能，即使有多个相同containerPort的容器，也可以同时容纳。
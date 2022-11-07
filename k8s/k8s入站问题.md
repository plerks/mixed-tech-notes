## k8s的入站问题

[k8s官网](https://kubernetes.io/zh/docs/concepts/services-networking/ingress/)有说明：

---

​	Ingress 可为 Service 提供外部可访问的 URL、负载均衡流量、终止 SSL/TLS，以及基于名称的虚拟托管。 [Ingress 控制器](https://kubernetes.io/zh/docs/concepts/services-networking/ingress-controllers) 通常负责通过负载均衡器来实现 Ingress，尽管它也可以配置边缘路由器或其他前端来帮助处理流量。Ingress 不会公开任意端口或协议。 将 HTTP 和 HTTPS 以外的服务公开到 Internet 时，通常使用 [Service.Type=NodePort](https://kubernetes.io/zh/docs/concepts/services-networking/service/#type-nodeport) 或 [Service.Type=LoadBalancer](https://kubernetes.io/zh/docs/concepts/services-networking/service/#loadbalancer) 类型的 Service。

​	你必须拥有一个[Ingress 控制器](https://kubernetes.io/zh/docs/concepts/services-networking/ingress-controllers) 才能满足 Ingress 的要求。 仅创建 Ingress 资源本身没有任何效果。

​    你可能需要部署 Ingress 控制器，例如 [ingress-nginx](https://kubernetes.github.io/ingress-nginx/deploy/)。 你可以从许多 [Ingress 控制器](https://kubernetes.io/zh/docs/concepts/services-networking/ingress-controllers) 中进行选择。

​    理想情况下，所有 Ingress 控制器都应符合参考规范。但实际上，不同的 Ingress 控制器操作略有不同。

---

以Service.Type=LoadBalancer为例，创建类似这样的service，

```
apiVersion: v1
kind: Service
metadata:
  annotations:
  labels:
    app.kubernetes.io/name: ingress-nginx
    app.kubernetes.io/part-of: ingress-nginx
  name: ingress-nginx-controller
  namespace: ingress-nginx
spec:
  type: LoadBalancer
  externalTrafficPolicy: Local
  ports:
    - name: http
      port: 80
      protocol: TCP
      targetPort: http
    - name: https
      port: 443
      protocol: TCP
      targetPort: https
    - name: proxied-tcp-3306
      port: 3306
      targetPort: 3306
      protocol: TCP
  selector:
    app.kubernetes.io/name: ingress-nginx
    app.kubernetes.io/instance: ingress-nginx
    app.kubernetes.io/component: controller
```

那么云服务商应该会提示用户为创建的LoadBalancer类型的service绑定公网ip(也就会是集群的入站ip)，并将TCP 80,443,3306的流量负载均衡到集群的这个service(无论云服务厂商如何实现)，除此之外这个service应该与普通service一致，有了这个service之后可以自己定义selector指定service选中的pod，那么不妨用这个service选中自己创建的nginx类型的pod，这样一来可以入站并通过nginx pod的能力按url转发请求(到集群内的实际业务service)。而[Ingress 控制器](https://kubernetes.io/zh/docs/concepts/services-networking/ingress-controllers/)应该就是做这样的事。



参考：

https://kubernetes.github.io/ingress-nginx/deploy/

https://www.servicemesher.com/blog/kubernetes-ingress-controller-deployment-and-ha/


安装了ingress controller之后，ingress controller原理上应该是这样的(参考<https://www.cnblogs.com/yuhaohao/p/13425439.html>)：

```
1）反向代理负载均衡器：通过接受并按照Ingress定义的规则进行转发，常用的有nginx，haproxy，traefik等。
2）ingress-nginx-controller：监听kube-apiserver，统计用户编写的ingress规则(用户编写的ingress的yaml文件)，动态的去更改nginx服务的配置文件，并且reload重载使其生效，此过程是自动的。
3）ingress：将nginx的配置抽象成一个Ingress对象，当用户每添加一个新的服务，只需要编写一个新的yaml文件即可。
```

也就是说，安装ingress controller的过程，实际上应该是安装了一个service(type=LoadBalancer)，一个deployment(这个deployment创建了类似nginx的pod(用来支持url转发)，这些nginx pod被service(type=LoadBalancer)选中)，以及添加了ingress支持(用户创建更新ingress时，将ingress里写的规则更新到nginx pod里(这个不知道怎么做的，可能是这些nginx pod自己会从api server获取ingress对象更新自己))。所以安装好ingress controller之后再写ingress定义转发规则就可以了。现在的路径就是：客户访问 -> 云服务商提供的带公网ip的服务器 -> type=LoadBalancer的service -> ingress controller创建的nginx pod，然后nginx pod再根据自己内部的规则将请求转发到其它集群内的服务，nginx内部的规则来自用户创建的ingress。

[Nginx Ingress Controller](https://kubernetes.github.io/ingress-nginx/deploy/)官网的有一种安装方式是`kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.1.2/deploy/static/provider/cloud/deploy.yaml`，查看这个yaml可以发现其中创建了这样一个Service：

```
apiVersion: v1
kind: Service
metadata:
  labels:
    app.kubernetes.io/component: controller
    app.kubernetes.io/instance: ingress-nginx
    app.kubernetes.io/managed-by: Helm
    app.kubernetes.io/name: ingress-nginx
    app.kubernetes.io/part-of: ingress-nginx
    app.kubernetes.io/version: 1.1.2
    helm.sh/chart: ingress-nginx-4.0.18
  name: ingress-nginx-controller
  namespace: ingress-nginx
spec:
  externalTrafficPolicy: Local
  ipFamilies:
  - IPv4
  ipFamilyPolicy: SingleStack
  ports:
  - appProtocol: http
    name: http
    port: 80
    protocol: TCP
    targetPort: http
  - appProtocol: https
    name: https
    port: 443
    protocol: TCP
    targetPort: https
  selector:
    app.kubernetes.io/component: controller
    app.kubernetes.io/instance: ingress-nginx
    app.kubernetes.io/name: ingress-nginx
  type: LoadBalancer
```

还有这样的Deployment:

```
apiVersion: apps/v1
kind: Deployment
metadata:
  labels:
    app.kubernetes.io/component: controller
    app.kubernetes.io/instance: ingress-nginx
    app.kubernetes.io/managed-by: Helm
    app.kubernetes.io/name: ingress-nginx
    app.kubernetes.io/part-of: ingress-nginx
    app.kubernetes.io/version: 1.1.2
    helm.sh/chart: ingress-nginx-4.0.18
  name: ingress-nginx-controller
  namespace: ingress-nginx
spec:
  minReadySeconds: 0
  revisionHistoryLimit: 10
  selector:
    matchLabels:
      app.kubernetes.io/component: controller
      app.kubernetes.io/instance: ingress-nginx
      app.kubernetes.io/name: ingress-nginx
  template:
    metadata:
      labels:
        app.kubernetes.io/component: controller
        app.kubernetes.io/instance: ingress-nginx
        app.kubernetes.io/name: ingress-nginx
    spec:
      containers:
      - args:
        - /nginx-ingress-controller
        - --publish-service=$(POD_NAMESPACE)/ingress-nginx-controller
        - --election-id=ingress-controller-leader
        - --controller-class=k8s.io/ingress-nginx
        - --ingress-class=nginx
        - --configmap=$(POD_NAMESPACE)/ingress-nginx-controller
        - --validating-webhook=:8443
        - --validating-webhook-certificate=/usr/local/certificates/cert
        - --validating-webhook-key=/usr/local/certificates/key
        env:
        - name: POD_NAME
          valueFrom:
            fieldRef:
              fieldPath: metadata.name
        - name: POD_NAMESPACE
          valueFrom:
            fieldRef:
              fieldPath: metadata.namespace
        - name: LD_PRELOAD
          value: /usr/local/lib/libmimalloc.so
        image: k8s.gcr.io/ingress-nginx/controller:v1.1.2@sha256:28b11ce69e57843de44e3db6413e98d09de0f6688e33d4bd384002a44f78405c
以下省略
```
也就是说，ingress controller确实创建了一个LoadBalancer类型的Service，同时，这个Service选中了其创建的类似nginx的pod，这个类似nginx的pod会根据用户创建的Ingress里指定的url转发规则，将流量转发到业务service。

上面的service只处理了http和https的TCP请求，如果还需要从外面访问其它端口的TCP服务，还需要自己在安装ingress controller后重写service，在ports字段增加端口，例如

```
apiVersion: v1
kind: Service
metadata:
  annotations:
  labels:
    app.kubernetes.io/name: ingress-nginx
    app.kubernetes.io/part-of: ingress-nginx
  name: ingress-nginx-controller
  namespace: ingress-nginx
spec:
  type: LoadBalancer
  externalTrafficPolicy: Local
  ports:
    - name: http
      port: 80
      protocol: TCP
      targetPort: http
    - name: https
      port: 443
      protocol: TCP
      targetPort: https
    - name: proxied-tcp-3306
      port: 3306
      targetPort: 3306
      protocol: TCP #这里不配tcp-services-configmap会有问题，因为到后端的nginx pod后因为数据库不是http的，没有路径信息来决定转发规则
  selector:
    app.kubernetes.io/name: ingress-nginx
    app.kubernetes.io/instance: ingress-nginx
    app.kubernetes.io/component: controller
```

此外Ingress默认不支持TCP or UDP services，因此Ingress controller使用--tcp-services-configmap和--udp-services-configmap这两个配置达到转发端口的目的(configmap的name必须是tcp-services和udp-services)。参考：<https://blog.51cto.com/u_15315026/3200029、https://zhuanlan.zhihu.com/p/460264913> 、<https://github.com/kubernetes/ingress-nginx/blob/main/docs/user-guide/exposing-tcp-udp-services.md>。配置里直接指定TCP要转到哪个service上，不走ingress里定义的http转发规则。

不过一般不应该把集群的3306端口暴露在集群外，应该是用config文件配置好了kubectl之后，用[kubectl port-forward](https://kubernetes.io/zh-cn/docs/tasks/access-application-cluster/port-forward-access-application-cluster/#%E8%BD%AC%E5%8F%91%E4%B8%80%E4%B8%AA%E6%9C%AC%E5%9C%B0%E7%AB%AF%E5%8F%A3%E5%88%B0-pod-%E7%AB%AF%E5%8F%A3)在本机创建代理，例如：`kubectl port-forward service/db-mysql-cluster-ip 6000:3306 -n mynamespace`，然后连本机的6000端口连接到数据库。

如果不是云服务商提供的k8s，要对外暴露服务的话就应该用type=NodePort的service，然后service后面用nginx pod来做url转发和负载均衡(和ingress controller的方式相同)，把不同的请求分发到不同的业务容器。

实际做的时候，只需要先装[nginx ingress controller](https://kubernetes.github.io/ingress-nginx/deploy/)，然后把nginx ingress controller创建的那个LoadBalancer类型的service手动改成NodePort类型的，并修改对应端口为80和443(要先[改NodePort的端口范围](https://kuboard.cn/install/install-node-port-range.html)，不然NodePort类型的service的port不能用80和443)。不过其实也可以只改端口，不把那个LoadBalancer类型的Service改成NodePort类型的，因为LoadBalancer既是ClusterIP，也是NodePort，不过不改成NodePort的话那个Service的EXTERNAL-IP会一直显示为pending(因为没云厂商提供公网ip)。这样操作下来集群的入口就是任意一个有公网ip的Node，不像云厂商环境下的LoadBalancer一样是通过外部的一个公网ip地址负载均衡到各个Node。

### 关于NodePort,ClusterIP,LoadBalancer

三种Service都会在所有节点上设置路由规则，只是设置的规则不一样，最终实现的效果不一样。

创建NodePort类型的Service(port范围由`--service-node-port-range`标志限定，默认30000-32767)，每个节点都会将对那个port的访问代理到容器中，最终的效果是每个节点(包括Master和Node)上都开了端口，<任一节点的任一ip>:<NodePort类型的Service指定的端口号>，都会访问到Service选中的pod。

但是k8s并不是在每个节点上起了个server，而是配置了iptables的转发规则，NodePort对应的端口不是有一个进程在监听，可以在Node上起一个那个端口的server，但是因为流量会先被iptables的规则转发，所以起的server会访问不到。

具体来说：参考<https://yuerblog.cc/2019/12/09/k8s-%E6%89%8B%E6%8A%8A%E6%89%8B%E5%88%86%E6%9E%90service%E7%94%9F%E6%88%90%E7%9A%84iptables%E8%A7%84%E5%88%99/>
，对于NodePort类型的service，转发规则就是：
```
dst ip是node自己，并且dst port是NodePort端口的流量，将其改写到service的某个endpoints
```
这里的dst ip，当node有多个ip地址时，任意一个ip:<NodePort端口>都可以用来访问(包括localhost)。因为配置规则是`--dst-type LOCAL`，参考<https://www.jianshu.com/p/f0dee39b20ba>：`LOCAL:表示地址是本地地址，指本地一切地址含：127.0.0.1回环地址`。

此外，NodePort类型的Service也会有个ClusterIP，[官网](https://kubernetes.io/docs/concepts/services-networking/service/#publishing-services-service-types)说明如下：
```
NodePort: Exposes the Service on each Node's IP at a static port (the NodePort). To make the node port available, Kubernetes sets up a cluster IP address, the same as if you had requested a Service of type: ClusterIP.
```

如果是ClusterIP，转发规则就是：
```
dst ip是ClusterIP(内网地址)的包，把dst ip改写成service的某个endpoints的ip地址。
```
所以ClusterIP类型的service只能在集群内用分配的ClusterIP访问

LoadBalancer按[参考链接](https://yuerblog.cc/2019/12/09/k8s-%E6%89%8B%E6%8A%8A%E6%89%8B%E5%88%86%E6%9E%90service%E7%94%9F%E6%88%90%E7%9A%84iptables%E8%A7%84%E5%88%99/)的说法既是NodePort，也是ClusterIP，[官网](https://kubernetes.io/zh-cn/docs/concepts/services-networking/service/#publishing-services-service-types)也有说:
```
LoadBalancer：使用云提供商的负载均衡器向外部暴露服务。外部负载均衡器可以将流量路由到自动创建的NodePort服务和ClusterIP服务上。
```
也就是说，LoadBalancer既是一个NodePort，也是一个ClusterIP，云厂商只需要检测到用户创建了LoadBalancer类型的Service后，分配一个公网ip，然后把这个公网ip的流量负载均衡到用户集群的各个Node上。

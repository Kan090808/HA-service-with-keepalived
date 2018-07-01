# HA-service-with-keepalived
用Keepalived達到服務高可用性
===

參考：
1. [Build Ftp Server](https://www.ostechnix.com/install-vsftpd-server-ubuntu-16-04-lts/)
2. [Keepalived介紹](http://blog.51cto.com/zephiruswt/1235852)
3. [keepalived 詳解](http://www.ywnds.com/?p=7963)
4. [Keepalived vs Heartbeat](https://blog.csdn.net/yunhua_lee/article/details/9788433)
5. [different](https://blog.csdn.net/fei33423/article/details/72513435)
6. [Set Up Highly Available Web Servers with Keepalived and Floating IPs](https://www.digitalocean.com/community/tutorials/how-to-set-up-highly-available-web-servers-with-keepalived-and-floating-ips-on-ubuntu-14-04)
7. [Simple keepalived failover setup](https://raymii.org/s/tutorials/Keepalived-Simple-IP-failover-on-Ubuntu.html)


## [Keepalived介绍](http://blog.51cto.com/zephiruswt/1235852)
Keepalived是一款高可用软件，它的功能主要包括两方面：
1. 通过IP漂移，实现服务的高可用：
   服务器集群共享一个虚拟IP，同一时间只有一个服务器占有虚拟IP并对外提供服务，若该服务器不可用，则虚拟IP漂移至另一台服务器并对外提供服务
2. 对LVS应用服务层的应用服务器集群进行状态监控：
   若应用服务器不可用，则keepalived将其从集群中摘除，若应用服务器恢复，则keepalived将其重新加入集群中。
![](https://i.imgur.com/ShZC894.png)

## Keepalived原理
Keepalived的实现基于VRRP（Virtual Router Redundancy Protocol，虚拟路由器冗余协议），而VRRP是为了解决静态路由的高可用。VRRP的基本架构如图所示：

#### VRRP简介（转发机制）
1. VRRP 将可以承担网关功能的路由器加入到备份组中，形成一台虚拟路由器，由VRRP的选举机制决定哪台路由器承担转发任务，局域网内的主机只需将虚拟路由器配置为缺省网关

2. VRRP是一种容错协议，在提高可靠性的同时，简化了主机的配置。在具有多播或广播能力的局域网（如以太网）中，借助VRRP 能在某台设备出现故障时仍然提供高可靠的缺省链路，有效避免单一链路发生故障后网络中断的问题，而无需修改动态路由协议、路由发现协议等配置信息

3. VRRP协议的实现有VRRPv2和VRRPv3两个版本，VRRPv2于IPv4，VRRPv3基     于IPv6

  如下图所示： 有两个路由器，两个网关，从两个路由器中选举出一个路由器作为主路由器，其他的都是备份路由器，主路由器负责发转发数据报，而备份路由器处于空闲状态，当主路由器出现故障后，备份路由器会成为主路由器，代替主路由器实现转发功能。
  
![](https://i.imgur.com/oOcCLkB.png)
虚拟路由器由多个VRRP路由器组成，每个VRRP路由器都有各自的IP和共同的VRID(0-255)，其中一个VRRP路由器通过竞选成为MASTER，占有VIP，对外提供路由服务，其他成为BACKUP，MASTER以IP组播（组播地址：224.0.0.18）形式发送VRRP协议包，与BACKUP保持心跳连接，若MASTER不可用（或BACKUP接收不到VRRP协议包），则BACKUP通过竞选产生新的MASTER并继续对外提供路由服务，从而实现高可用。

## [Keepalived vs Heartbeat](https://blog.csdn.net/yunhua_lee/article/details/9788433)
1. Keepalived使用更简单：
   从安装、配置、使用、维护等角度上对比，Keepalived都比Heartbeat要简单得多，尤其是Heartbeat 2.1.4后拆分成3个子项目，安装、配置、使用都比较复杂，尤其是出问题的时候，都不知道具体是哪个子系统出问题了；而Keepalived只有1个安装文件、1个配置文件，配置文件也简单很多；

2. Heartbeat功能更强大：
   Heartbeat虽然复杂，但功能更强大，配套工具更全，适合做大型集群管理，而Keepalived主要用于集群倒换，基本没有管理功能；

3. 协议不同：
   Keepalived使用VRRP协议进行通信和选举，Heartbeat使用心跳进行通信和选举；Heartbeat除了走网络外，还可以通过串口通信，貌似更可靠；

4. 使用方式基本类似：
   如果要基于两者设计高可用方案，最终都要根据业务需要写自定义的脚本，Keepalived的脚本没有任何约束，随便怎么写都可以；Heartbeat的脚本有约束，即要支持service start/stop/restart这种方式，而且Heartbeart提供了很多默认脚本，简单的绑定ip，启动apache等操作都已经有了；

### [差別](https://blog.csdn.net/fei33423/article/details/72513435)
keepalived:  (尽量保持可用)
確保Virtual ip 至少在一個伺服器上，持續提供服務

heart beat: (尽量保持唯一性存储)
提供共享文件系统,确保共享资源最多存在于一个位置。共享资源不会被同时访问。这是一项非常艰巨的任务，而且它做得很好。
## [實作](https://raymii.org/s/tutorials/Keepalived-Simple-IP-failover-on-Ubuntu.html)
![](https://i.imgur.com/hYYgc4I.png)


兩臺實體伺服器各裝有apache2和ftp server
apache2： 顯示進入哪一臺伺服器
(可透過`ip addr list wlp3s0` 顯示是否取得Virtual IP)

FTP Server：提供服務，用nfs跟主機同步
```
$ apt-get install nginx keepalived

$ vim /etc/keepalived/keepalived.conf
//first server
vrrp_instance VI_1 {
    state MASTER
    interface wlp3s0
    virtual_router_id 51
    priority 200
    advert_int 1
    authentication {
        auth_type PASS
        auth_pass 00000000
    }
    virtual_ipaddress {
        192.168.43.190
    }
}
//second server
vrrp_instance VI_1 {
    state BACKUP
    interface wlp3s0
    virtual_router_id 51
    priority 100
    advert_int 1
    authentication {
        auth_type PASS
        auth_pass 00000000
    }
    virtual_ipaddress {
        192.168.43.190
    }
}

$ vim /etc/sysctl.conf
add this line : net.ipv4.ip_nonlocal_bind = 1
//  綁定一組非本地的ip，以免被本地佔用ip

$ sysctl -p
```
### 小demo
{%youtube lCYEdrSsaGs %}
擴展功能：
1. LVS
2. 包裝成docker(包括網卡和ip設定自動化)

```
// 補充
// vsftpd 更改root路徑
sudo vim /etc/vsftpd.conf
    local_root=/nfs
    
// list all network card name
ifconfig -a | sed 's/[ \t].*//;/^$/d'

//get ip addre by network card name
/sbin/ip -o -4 addr list wlp3s0 | awk '{print $4}' | cut -d/ -f1


```


### zookeeper 监控工具

主要用于开发人员做数据查看的,可以在上面看 dubbo 的提供者信息，kafka 主题，分区，消费者信息，如果使用 zk 做分布式锁，也可以在上面看锁的信息

#### 背景

此工具是为了模仿 ZooInspector 做的，最初的代码也全是 ZooInspector 的代码，后面进行了缩减。ZooInspector 一个实例只支持单连接，而界面端是可以做多连接的，而且 ZooInspector 在数据多的时候会卡死，但网页端的不会
### kafka 消费监控

用于监控消费组的每个主题的消费情况

可以单独对某一个消费组的某一个主题进行监控

可以查看某一主题,某一分区,目前的最新数据

旧版 kafka 0.8.1.1 之前的,可以设置 offset 来重新消费

#### 背景

写这个工具的原因主要是由于公司用的 kafkaOffsetMonitor 不够好用。公司还是用的旧版本的 kafka ,offset 是保存在 zookeeper 上的

每次读取 offset ，需要把当前消费组的所有主题的 offset 刷新一次，并且时不时会卡死；还只能查询，不能设置 zookeeper 的 offset ,每次使用命令去设置特别麻烦。

#### 功能使用

重点说下如何新建连接,第一个参数是需要选择的

新建 kafka 连接的时候，你必须要有一个 zookeeper 连接，需要先去 zookeeper 监控中添加一个 zookeeper 连接

然后才可以在新建 kafka 连接中选择 zk 连接

版本的选择在 0.8.1.1（包含） 之前选择旧版本，之后选择新版本即可



* 选择连接后，将刷新出所有消费组

![](http://pic.yupoo.com/sanri1993/7c2f3fc5/fe5c9295.png)

* 点击你的消费组，查询出当前消费组消费的所有主题及主题的总的剩余量，日志大小相关信息

![](http://pic.yupoo.com/sanri1993/87773619/81c9177e.png)

* 点击监控，进入当前消费组，选择的主题的每个分区的消费信息详情

![](http://pic.yupoo.com/sanri1993/8430b3a4/64991a8c.png)



* 点击附近数据，将查询当前 offset 前 100 条，后 100 条的数据； 查尾部数据，将查询 logSize 往前 100 条数据
* 可以选择不同的序列化格式来序列化数据，我这里是 json 格式，使用 string 即可



![](http://pic.yupoo.com/sanri1993/6b8e9dde/87e26451.png)



* 当数据格式为 json 时可以点击 json 书，以 json 数据查看

![](http://pic.yupoo.com/sanri1993/53e12480/0ac54c1d.png)
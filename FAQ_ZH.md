# FAQ

**Q1: Saga的执行是同步的还是异步的？发起Saga之后，是等所有Sub-transaction都完成才返回，还是立即返回？**

目前Saga事情的执行是同步的，后续我们会提供异步方式的实现。


**Q2: Saga是并行还是顺序执行Sub-transaction的？**

Saga pack是根据调用的代码来决定Saga事件，如果Saga子事件是并行方式调用的， 那Saga协调器也是采用并行方式进行处理的。


**Q3: 对于@Compensable方法及compenstation方法有什么要求？**

有以下要求：

1. 这两个方法幂等。
1. 这两个方法的参数列表完全一致。
1. 这两个方法在写在同一个类中。
1. 参数要能够序列化。
1. 这两个方法是可交换的，即如果参数相同，这两个方法无论以什么顺序执行结果都是一样的。


**Q4: Saga保证ACID吗？**

Saga保证原子性（Atomicity）、一致性（Consistency）、持久性（Durability），但不保证隔离性（Isolation）。


**Q5: Saga可以嵌套吗？**

Saga实现支持子事件嵌套的方式。


**Q6: 如何水平扩展Saga Alpha？**

Saga Alpha在设计过程中状态信息都存储到数据库，是支持水平扩展的。


**Q7: 在执行Saga过程中某个服务崩溃了，那么重启后Saga还会继续执行吗？**

在服务重启后，Saga Alpha会尝试继续执行Saga。

**Q8: 如何使用MySQL作为alpha的后台数据库？**

参阅这篇[文档](docs/faq/cn/how_to_use_mysql_as_alpha_backend_database.md)



**问题描述:** Omega客户端不执行Alpha服务端发出的命令
 
帮忙看下这个问题，Omega端能处理正常的调用，但是不执行Alpha服务端发送的指令， Alpha服务器没报错。
服务的调用参数如下：使用字符串或者基本类型，还有map来传参数。
我将参数涉及到的bean jar加入到Alpha中，Omega端还是报错，使用字符串或者基本类型，还有map来传参数，调用补偿方法都没问题。

报错信息：
```
2019-01-07 19:25:39.983 WARN 18736 --- [nio-6060-exec-2] .m.m.a.ExceptionHandlerExceptionResolver : Resolved exception caused by handler execution: cn.com.aiidc.hicloud.component.bean.exception.HicloudException
2019-01-07 19:25:40.351 INFO 18736 --- [ault-executor-4] s.p.o.c.g.s.GrpcCompensateStreamObserver : Received compensate command, global tx id: 31c67d8e-1ced-4f70-83b6-7c151186120f, local tx id: a5cc191d-e50b-4c69-bb53-bb61309a1049, compensation method: public boolean cn.com.aiidc.hicloud.demo.service.impl.OrderServiceImpl.cancelCreateOrder(cn.com.aiidc.hicloud.component.bean.vo.order.CreateOrderVo)
2019-01-07 19:25:40.357 ERROR 18736 --- [ault-executor-4] o.a.s.p.o.c.g.c.ReconnectStreamObserver : Failed to process grpc coordinate command.
io.grpc.StatusRuntimeException: CANCELLED: Failed to read message.
at io.grpc.Status.asRuntimeException(Status.java:526) ~[grpc-core-1.14.0.jar:1.14.0]
at io.grpc.stub.ClientCalls$StreamObserverToCallListenerAdapter.onClose(ClientCalls.java:420) [grpc-stub-1.14.0.jar:1.14.0]
A
```
解答：

因为Alpha不会做反序列化的操作，其实添加cn.com.aiidc.hicloud.component.bean.vo.order.CreateOrderVo 这个类到Alpha中意义不大。

经过交流发现引入如下的依赖后造成的问题

```
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-devtools</artifactId>
  <optional>true</optional>
</dependency>
```

maven去掉这个之后补偿方法正常调用了。详情参考： https://docs.spring.io/spring-boot/docs/current/reference/html/using-boot-devtools.html#using-boot-devtools-known-restart-limitations

点评：引入spring-boot-devtools 会修改Omega对应的classloader，这样会造成omega寻找对应方法签名时出错。

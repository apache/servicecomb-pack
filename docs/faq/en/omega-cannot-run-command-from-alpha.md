
**Problem:** Omega does not run the command from Alpha
 
We have a problem that Omega can pass the message to Alpha, but Omega cannot execute the command from Alpha. There is no error log from Alpha side, Omega keep complain about it failed to read the message from Alpha.
I tried to add the Omega method related module classes into Alpha, but Omega is keeping failed. The parameter are String and some other basic type, and we also use map in the parameter.

Here is the stack trace from Omega side：

```
2019-01-07 19:25:39.983 WARN 18736 --- [nio-6060-exec-2] .m.m.a.ExceptionHandlerExceptionResolver : Resolved exception caused by handler execution: cn.com.aiidc.hicloud.component.bean.exception.HicloudException
2019-01-07 19:25:40.351 INFO 18736 --- [ault-executor-4] s.p.o.c.g.s.GrpcCompensateStreamObserver : Received compensate command, global tx id: 31c67d8e-1ced-4f70-83b6-7c151186120f, local tx id: a5cc191d-e50b-4c69-bb53-bb61309a1049, compensation method: public boolean cn.com.aiidc.hicloud.demo.service.impl.OrderServiceImpl.cancelCreateOrder(cn.com.aiidc.hicloud.component.bean.vo.order.CreateOrderVo)
2019-01-07 19:25:40.357 ERROR 18736 --- [ault-executor-4] o.a.s.p.o.c.g.c.ReconnectStreamObserver : Failed to process grpc coordinate command.
io.grpc.StatusRuntimeException: CANCELLED: Failed to read message.
at io.grpc.Status.asRuntimeException(Status.java:526) ~[grpc-core-1.14.0.jar:1.14.0]
at io.grpc.stub.ClientCalls$StreamObserverToCallListenerAdapter.onClose(ClientCalls.java:420) [grpc-stub-1.14.0.jar:1.14.0]
A
```
Solution：

Because of the Alpha does not unmarshal the message of the method parameter, adding the module classes into Alpha won't do any help.

After talking with the user, we found it was caused by the dependency of below:

```
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-devtools</artifactId>
  <optional>true</optional>
</dependency>
```

The issue is gone when we remove the dependency from pom.
After checking the information https://docs.spring.io/spring-boot/docs/current/reference/html/using-boot-devtools.html#using-boot-devtools-known-restart-limitations
We found spring-boot-devtools will modify the classloader of Omega, it cause the trouble that omega cannot find the invocation method information when it receives the command from Alpha。

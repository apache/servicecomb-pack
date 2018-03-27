# FAQ

**Q1: Is Saga executed synchronously or asynchronously？Will Saga not be finished until all sub-transaction finished?**

Currently Saga is executed synchronously. Asynchronous mode will be implemented in future.


**Q2: How are sub-transactions executed by Saga, in parallel or in sequence?**

How Saga pack executes sub-transaction depends on your code. If you call `@Compensable` method in parallel, then Saga Alpha will process in parallel. 


**Q3: Any requirements on `@Compensable` method and compenstation method？**

Here are the requirements：

1. Both methods should be idempotent.
1. Both methods have exactly same parameter list.
1. Both methods are in same class.
1. Parameter should be serializable.
1. They should be commutative. For example, if given same parameters, the result should be same regardless the order of execution.


**Q4: Does Saga guarantee ACID?**

Saga guarantees Atomicity, Consistency, Durability, but not guarantee Isolation。


**Q5: Can Saga be nested?**

Yes, Saga can be nested.


**Q6: How to scale Saga Alpha horizontally?**

Saga Alpha is stateless, all data is persisted in database, so it supports horizontal scale.


**Q7: Will Saga continue if some services crashed in execution?**

Saga Alpha will try to continue Saga execution after service restart.


**Q8: How to use MySQL as alpha's backend database?**

Check this [doc](docs/faq/en/how_to_use_mysql_as_alpha_backend_database.md)

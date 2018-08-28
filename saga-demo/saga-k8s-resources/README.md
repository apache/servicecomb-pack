# Deploy & test saga with Kubernetes

You can now play with saga under Kubernetes!

The demos' Kubernetes resources are splitted into 3 categories:

- **base**: The base resources that all demos needs under `base/` folder, including the `alpha-server,`the `postgresql` database, and a `jmeter-collector` for [load test](https://github.com/apache/incubator-servicecomb-saga/blob/master/docs/load_test.md).
- **demos**: The resources that each demo will consume, stored in the folder named with `xxx-demo/`
- **tests**: A [jmeter](https://jmeter.apache.org/) deployment is provided for each demo under the demo's `test/` folder, there is also a [Kubernetes configmap](http://kubernetes-v1-4.github.io/docs/user-guide/configmap/) where you can change and apply the jmeter test plans. So you can change the plan at any time and pull up the jmeter deployment to test the demos.



## Get started

All the Kubernetes objects will be deployed in the `servicecomb` namespace, so make sure it is there with the command:

```bash
$ kubectl create namespace servicecomb
namespace "servicecomb" created
```

Let's take the `spring-demo` as the example, apply the base and demo resources:

```bash
$ kubectl apply -f ./base
service "alpha-server" created
deployment "alphaserver" created
service "postgresql" created
deployment "database" created
service "jmeter-collector" created
deployment "jmeter-collector" created

$ kubectl apply -f ./spring-demo
service "booking" created
deployment "booking" created
service "car" created
deployment "car" created
service "hotel" created
deployment "hotel" created

```

Make sure all the services in the demo are up and running:

```bash
$ kubectl get pods -n servicecomb
NAME                           READY     STATUS    RESTARTS   AGE
alphaserver-6cb48898fc-tlqtb   1/1       Running   0          1d
booking-666c8f4dbb-k5glt       1/1       Running   0          4h
car-975d666f8-prs9p            1/1       Running   0          1d
database-796fc68b98-mhz8n      1/1       Running   0          1d
hotel-768b59dfcd-gc7sn         1/1       Running   0          1d
```

Then you can test the demo with jmeter, modify the configmap according to your test plan, then apply the test service to Kubernetes api server:

```bash
$ vim ./spring-demo/test/jmeter.configmap.yaml
$ # edit the file as you wish, mind the identation of yamls
$ kubectl apply -f ./spring-demo/test/
configmap "springdemo-jmeter-script" created
deployment "spring-demo-jmeter" created

$ kubectl get pods -n servicecomb | grep jmeter
jmeter-collector-75c4c96dbb-2bjtc     1/1       Running   0          47s
spring-demo-jmeter-6cfb679f58-sckkx   1/1       Running   0          12s
```

The jmeter deployment will keep testing the demo and generate the test result. After each test, the result and its corresponding dashboard will be uploaded to the jmeter-collector service, which provides a simple web page to browser the test outputs. So make sure you config the jmeter-collector to be accessible, like a [NodePort](https://kubernetes.io/docs/concepts/services-networking/service/#nodeport) in a private cluster or [LoadBalancer](https://kubernetes.io/docs/concepts/services-networking/#loadbalancer) in a public cloud service.

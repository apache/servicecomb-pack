# Deploy & test saga with Kubernetes

You can now play with saga under Kubernetes!

The demos' Kubernetes resources are splitted into 3 categories:

- **base**: The base resources that all demos needs under `base/` folder, including the `alpha-server` and `postgresql` database
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
spring-demo-jmeter-6cfb679f58-sckkx   1/1       Running   0          12s
```

The jmeter deployment will keep testing the demo and generate the test result. For now the the result file is stored in the Kubernetes nodes under the path `/saga-jmeter-result/{demo_name}.jtl`. You can generate the HTML dashboard with the command:

```bash
$ jmeter -g /saga-jmeter-result/{demo_name}.jtl -o output/
```

We will try to provide more services to automate the dashboard generation in the future.

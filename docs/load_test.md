### Load testing saga with Kubernetes

First make sure a [saga-demo](https://github.com/apache/servicecomb-saga/tree/master/saga-demo) is running in the kubernetes cluster(you can find useful info at [saga's k8s demo](https://github.com/apache/servicecomb-saga/tree/master/saga-demo/saga-k8s-resources)), here we take the [saga-spring-demo](https://github.com/apache/servicecomb-saga/tree/master/saga-demo/saga-spring-demo) as an example.

Navigate to `saga-demo/saga-k8s-resources/spring-demo/test`, there is a jmeter service and corresponding configmap where jmeter's

There is a jmeter service definition in jmeter.yaml, and a configmap where the jmeter service's config is stored. We first deploy the configmap in the cluster:

```bash
$ kubectl aplly -f ./jmeter.config.yaml
configmap "springdemo-jmeter-script" created
```

So the configmap is there in the cluster, we can edit it any time with `kubectl edit`:

```bash
$ kubectl edit configmap springdemo-jmeter-script -n servicecomb
```

Adjust the testing parameters, you may be interested in `ThreadGroup.num_threads`, `ThreadGroup.ramp_time`, `ThreadGroup.duration`, which are: the number of max testing threads, the duration to start the max threads and the total testing time.

Then we deploy the jmeter service:

```bash
$ kubectl apply -f ./jmeter.yaml
```

If you're interested in what's going on inside the service, open the jmeter.yaml file, and you'll see the configmap is delivered to jmeter through a volume mount:

```yaml
# ...
containers:
- name: spring-demo-jmeter
env:
- name: REPORT_UPLOAD_SERVER
  value: jmeter-collector.servicecomb
# ...
  volumeMounts:
  - name: jmeter-script
    mountPath: /tmp/
volumes:
  - name: jmeter-script
    configMap:
      name: springdemo-jmeter-script
```

What's more, when we deploy the resources in spring-demo's base folder, a `jmeter-collector` service is started, it will receive the jmeter service's uploading archives. The collector's upload URL is defined in environment variable `REPORT_UPLOAD_SERVER`.

Since the jmeter service's instance is defined in a [kubernetes deployment](https://kubernetes.io/docs/concepts/workloads/controllers/deployment/), the restartPolicy will be 'Always', it will keep testing, uploading, exiting, restarting again and again. When we want to adjust the load test parameter, we edit the configmap with `kubectl edit`, save it, restart the `jmeter` deployment and test with the new parameters.

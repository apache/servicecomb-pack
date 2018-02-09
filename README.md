# Saga | [中文](README_ZH.md) [![Build Status](https://travis-ci.org/apache/incubator-servicecomb-saga.svg?branch=master)](https://travis-ci.org/apache/incubator-servicecomb-saga?branch=master) [![Coverage Status](https://coveralls.io/repos/github/apache/incubator-servicecomb-saga/badge.svg?branch=master)](https://coveralls.io/github/apache/incubator-servicecomb-saga?branch=master) [![License](https://img.shields.io/badge/license-Apache%202-4EB1BA.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)
Apache ServiceComb (incubating) Saga is an eventually data consistency solution for micro-service applications. Transactions are commited directly in the try phase and compensated in reverse order in the rollback phase comparing to [TCC](http://design.inf.usi.ch/sites/default/files/biblio/rest-tcc.pdf). 

## Features
* High availability. The coordinator is stateless and thus can have multiple instances.
* High reliability. All transaction events are stored in database permanently.
* High performance. Transaction events are reported to coordinator via gRPC and transaction payloads are serialized/deserialized by Kyro.
* Low invasion. All you need to do is add 2-3 annotations and the corresponding compensate methods.
* Easy to deploy. All components can boot via docker.
* Support both forward(retry) and backward(compensate) recovery.

## Architecture
Saga is composed of  **alpha** and **omega**.
* The alpha plays as the coordinator. It is responsible for the management of transactions.
* The omega plays as an agent inside the micro-service. It intercepts incoming/outgoing requests and reports transaction events to alpha.

The following diagram shows the relationships among alpha, omega and services.
![Saga Pack Architecture](docs/static_files/pack.png)

See [Saga Pack Design](docs/design.md) for details. If you are interested in our previous architecture, please move forward to [Old Saga's Documentation](docs/old_saga.md).

## Get Started
See [Booking Demo](saga-demo/pack-demo/README.md) for details.

## Contact Us
* [issues](https://issues.apache.org/jira/browse/SCB)
* [gitter](https://gitter.im/ServiceCombUsers/Lobby)
* mailing list: [subscribe](mailto:dev-subscribe@servicecomb.incubator.apache.org) [view](https://lists.apache.org/list.html?dev@servicecomb.apache.org)

## Contributing
See [Pull Request Guide](http://servicecomb.incubator.apache.org/developers/submit-codes/) for details.

## License
Licensed under an [Apache 2.0 license](https://github.com/apache/incubator-servicecomb-saga/blob/master/LICENSE).

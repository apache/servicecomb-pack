# Pack | [中文](README_ZH.md) [![Build Status](https://github.com/apache/servicecomb-pack/actions/workflows/master-push-build.yaml/badge.svg?branch=master)](https://github.com/apache/servicecomb-pack/actions/workflows/master-push-build.yaml?query=branch%3Amaster) [![Coverage Status](https://coveralls.io/repos/github/apache/servicecomb-pack/badge.svg?branch=master)](https://coveralls.io/github/apache/servicecomb-pack?branch=master) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.apache.servicecomb.pack/pack/badge.svg)](http://search.maven.org/#search%7Cga%7C1%7Corg.apache.servicecomb.pack) [![License](https://img.shields.io/badge/license-Apache%202-4EB1BA.svg)](https://www.apache.org/licenses/LICENSE-2.0.html) [![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=servicecomb-pack&metric=alert_status)](https://sonarcloud.io/dashboard?id=servicecomb-pack) [![Gitter](https://img.shields.io/badge/ServiceComb-Gitter-ff69b4.svg)](https://gitter.im/ServiceCombUsers/Saga)
Apache ServiceComb Pack is an eventually data consistency solution for micro-service applications.

## Features
* High availability. The coordinator is stateless and thus can have multiple instances.
* High reliability. All transaction events are stored in database permanently.
* High performance. Transaction events are reported to coordinator via gRPC and transaction payloads are serialized/deserialized by Kyro.
* Low invasion. All you need to do is add 2-3 annotations and the corresponding compensate methods.
* Easy to deploy. All components can boot via docker.
* Support both forward(retry) and backward(compensate) recovery.
* Easy to extend other coordination protocol which is based on the Pack, now we have Saga and TCC support out of box.

## Architecture
ServiceComb Pack is composed of  **alpha** and **omega**.
* The alpha plays as the coordinator. It is responsible for the management of transactions.
* The omega plays as an agent inside the micro-service. It intercepts incoming/outgoing requests and reports transaction events to alpha.


The following diagram shows the relationships among alpha, omega and services.
![Pack Architecture](docs/static_files/pack.png)

In this way, we can implement different coordination protocols, such as saga and TCC. See [ServiceComb Pack Design](docs/design.md) for details.

Now we have different lanaguage implementation of Omega
* Go lang version of Omega here https://github.com/jeremyxu2010/matrix-saga-go
* C# version of Omega here https://github.com/OpenSagas-csharp/servicecomb-saga-csharp

## Get Started
* For ServiceComb Java Chassis application, please see [Booking Demo](demo/saga-servicecomb-demo/README.md) for details.
* For Spring applications, please see [Booking Demo](demo/saga-spring-demo/README.md) for details.
* For Dubbo applications, please see [Dubbo Demo](demo/saga-dubbo-demo/README.md) for details.
* For TCC with Spring application, please see [Tcc Demo](demo/tcc-spring-demo/README.md) for details.
* To debug the applications, please see [Spring Demo Debugging](demo/saga-spring-demo#debugging) for details.

## Build and Run the tests from source

* Build the source code and run the tests
   ```bash
      $ mvn clean install
   ```
* Build the source demo docker images and run the accept tests
   ```bash
      $ mvn clean install -Pdemo
   ```
* Build the source code and docker images without running tests, the docker profile will be activated if the maven detects the docker installation.
   ```bash
      $ mvn clean install -DskipTests=true -Pdemo
   ```
* Build the release kit for distribution without running the tests, then you can find the release kits in the distribution/target directory.
  ```bash
     $ mvn clean install -DskipTests=true -Prelease
  ```  

## User Guide
How to build and use can refer to [User Guide](docs/user_guide.md).

## Get The Latest Version

Get released version:

* [Download Pack](http://servicecomb.apache.org/release/pack-downloads/)

Get snapshot version:

*  We publish the snapshot version to Apache nexus repo, please add below repositories into your pom.xml.
   ```
           <repositories>
             <repository>
               <releases />
               <snapshots>
                 <enabled>true</enabled>
               </snapshots>
               <id>repo.apache.snapshot</id>
               <url>https://repository.apache.org/content/repositories/snapshots/</url>
             </repository>
           </repositories>
           <pluginRepositories>
             <pluginRepository>
               <releases />
               <snapshots>
                 <enabled>true</enabled>
               </snapshots>
               <id>repo.apache.snapshot</id>
               <url>https://repository.apache.org/content/repositories/snapshots/</url>
             </pluginRepository>
           </pluginRepositories>


   ```    

## [FAQ](FAQ.md)

## Contact Us
* [issues](https://issues.apache.org/jira/browse/SCB)
* [gitter](https://gitter.im/ServiceCombUsers/Saga)
* mailing list: [subscribe](mailto:dev-subscribe@servicecomb.apache.org) [view](https://lists.apache.org/list.html?dev@servicecomb.apache.org)

## Contributors
[![contributors](https://badges.implements.io/api/contributors?org=apache&repo=servicecomb-pack&width=1280&size=48&padding=6&type=jpeg)](https://github.com/apache/servicecomb-pack/graphs/contributors)

## How to Contribute
See [Pull Request Guide](http://servicecomb.apache.org/developers/submit-codes/) for details.


## License
Licensed under an [Apache 2.0 license](https://github.com/apache/servicecomb-pack/blob/master/LICENSE).

## Export Notice

This distribution includes cryptographic software. The country in which you currently reside may have restrictions on the import, possession, use, and/or re-export to another country, of encryption software. BEFORE using any encryption software, please check your country's laws, regulations and policies concerning the import, possession, or use, and re-export of encryption software, to see if this is permitted. See <http://www.wassenaar.org/> for more information.

The Apache Software Foundation has classified this software as Export Commodity Control Number (ECCN) 5D002, which includes information security software using or performing cryptographic functions with asymmetric algorithms. The form and manner of this Apache Software Foundation distribution makes it eligible for export under the "publicly available" Section 742.15(b) exemption (see the BIS Export Administration Regulations, Section 742.15(b)) for both object code and source code.

The following provides more details on the included cryptographic software:
* Omega/Alpha Transport can be configured use https

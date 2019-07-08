/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.servicecomb.pack.omega.connector.grpc;

public class AlphaClusterDiscovery {

    private DiscoveryType discoveryType = DiscoveryType.DEFAULT;;

    private String[] addresses;

    private String discoveryInfo;

    public DiscoveryType getDiscoveryType() {
        return discoveryType;
    }

    public void setDiscoveryType(DiscoveryType discoveryType) {
        this.discoveryType = discoveryType;
    }

    public String[] getAddresses() {
        return addresses;
    }

    public void setAddresses(String[] addresses) {
        this.addresses = addresses;
    }

    public String getDiscoveryInfo() {
        return discoveryInfo;
    }

    public void setDiscoveryInfo(String discoveryInfo) {
        this.discoveryInfo = discoveryInfo;
    }

    public enum DiscoveryType{
        DEFAULT,EUREKA,CONSUL,ZOOKEEPER, NACOS
    }

    public static final Builder builder(){
        return new Builder();
    }

    public static final class Builder {
        private DiscoveryType discoveryType = DiscoveryType.DEFAULT;;
        private String[] addresses;
        private String discoveryInfo;

        public Builder discoveryType(DiscoveryType discoveryType) {
            this.discoveryType = discoveryType;
            return this;
        }

        public Builder discoveryInfo(String discoveryInfo) {
            this.discoveryInfo = discoveryInfo;
            return this;
        }

        public Builder addresses(String[] addresses) {
            this.addresses = addresses;
            return this;
        }

        public AlphaClusterDiscovery build() {
            AlphaClusterDiscovery alphaClusterDiscovery = new AlphaClusterDiscovery();
            alphaClusterDiscovery.setDiscoveryType(discoveryType);
            alphaClusterDiscovery.setAddresses(addresses);
            alphaClusterDiscovery.setDiscoveryInfo(discoveryInfo);
            return alphaClusterDiscovery;
        }
    }
}

# Enable TLS for omega-alpha communication

Saga now supports TLS for communication between omega and alpha server. Client side authentication(Mutual authentication) is also supported.

## Prepare Certificates

You can use the following commands to generate self-signed certificates for testing.

The client certificates is only needed if you want to use mutual authentication.


```
# Changes these CN's to match your hosts in your environment if needed.
SERVER_CN=localhost
CLIENT_CN=localhost # Used when doing mutual TLS

echo Generate CA key:
openssl genrsa -passout pass:1111 -des3 -out ca.key 4096
echo Generate CA certificate:
# Generates ca.crt which is the trustCertCollectionFile
openssl req -passin pass:1111 -new -x509 -days 365 -key ca.key -out ca.crt -subj "/CN=${SERVER_CN}"
echo Generate server key:
openssl genrsa -passout pass:1111 -des3 -out server.key 4096
echo Generate server signing request:
openssl req -passin pass:1111 -new -key server.key -out server.csr -subj "/CN=${SERVER_CN}"
echo Self-signed server certificate:
# Generates server.crt which is the certChainFile for the server
openssl x509 -req -passin pass:1111 -days 365 -in server.csr -CA ca.crt -CAkey ca.key -set_serial 01 -out server.crt 
echo Remove passphrase from server key:
openssl rsa -passin pass:1111 -in server.key -out server.key
echo Generate client key
openssl genrsa -passout pass:1111 -des3 -out client.key 4096
echo Generate client signing request:
openssl req -passin pass:1111 -new -key client.key -out client.csr -subj "/CN=${CLIENT_CN}"
echo Self-signed client certificate:
# Generates client.crt which is the clientCertChainFile for the client (need for mutual TLS only)
openssl x509 -passin pass:1111 -req -days 365 -in client.csr -CA ca.crt -CAkey ca.key -set_serial 01 -out client.crt
echo Remove passphrase from client key:
openssl rsa -passin pass:1111 -in client.key -out client.key
echo Converting the private keys to X.509:
# Generates client.pem which is the clientPrivateKeyFile for the Client (needed for mutual TLS only)
openssl pkcs8 -topk8 -nocrypt -in client.key -out client.pem
# Generates server.pem which is the privateKeyFile for the Server
openssl pkcs8 -topk8 -nocrypt -in server.key -out server.pem
```

## Enable TLS for Alpha Server

1. Edit the application.yaml file for alpha-server, add the ssl configuration under the `alpha.server` section.

```
alpha:
  server:
    ssl:
      enable: true
      cert: server.crt
      key: server.pem
      mutualAuth: true
      clientCert: client.crt
```

2. Put the server.crt and server.pem files under the root directory of the alpha-server. If you want to use mutual authentication, Merge all the client certificates into one file client.crt, then put the client.crt under the root directory. 

3. Restart alpha-server.


## Enable TLS for Omega

1. Get the CA certificate chain, you may need to merge multiple CA certificates into one file if you are running alpha server in cluster.

2. Edit the application.yaml file for the client application, add the ssl configuration under the `alpha.cluster` section.

```
alpha:
  cluster:
    address: alpha-server.servicecomb.io:8080
    ssl:
      enable: false
      certChain: ca.crt
      mutualAuth: false
      cert: client.crt
      key: client.pem
```
3. Put the ca.crt file under the client application root directory. If you want to use mutual authentication, also put the client.crt and client.pem under the root directory.

4. Restart the client application.


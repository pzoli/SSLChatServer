# SSLChatServer
Java chat server with SSL encryption

# Package and run
After maven package run server with following command:
```
java -jar target\SSLChatServer-0.0.1-SNAPSHOT-jar-with-dependencies.jar -host localhost -port 5441 -serverjks .keystore -trustjks trusted.jks  -serverpwd changeit -trustpwd changeit
```

# JMX readOnly and readWrite access control

JMX has a simple inbuilt authentication and authorization mechanism using password and access files. Roles can be specified with either readOnly or readWrite privileges.

readOnly does not allow to change values or to invoke operations but can monitor

readWrite gives the total control over the server which allows to change 
values and invoke operations

* Both password and access files are properties based text files for specifying username/password pairs and username/access pairs respectively.

* The password and access files could reside anywhere in the server and should be provided upon server startup.

* SSL is enabled by default for remote monitoring and thus the SSL should be configured properly after setting up a digital certificate. System properties for keystore and truststore should be set in the server.

This code is based on the Oracle documentation on [Java SE Monitoring and Management Guide](http://docs.oracle.com/javase/7/docs/technotes/guides/management/toc.html)

## Running the sample

1. Download the source code.
2. Open the commandline and move to `sample` directory
3. Compile all java files



        javac com/example/*.java
4. Run the java application Server.java from the command line using the following command
    
        java -Dcom.sun.management.jmxremote.port=9999 \
        -Djavax.net.ssl.keyStore=keystore \
        -Djavax.net.ssl.keyStorePassword=admin123 \
        com.example.Server
5. Open a new command line window/tab and run the jConsole using the following command

        jconsole -J-Djavax.net.ssl.trustStore=truststore \
        -J-Djavax.net.ssl.trustStorePassword=admin123
6. In jConsole, select romote process options and type in `localhost:3000` using provided credentials:
username: `monitorRole` and password: `QED` or username: `controlRole` and password: `R&D`
7. On the MBeans tab under `com.example > hello`, try out changing the value of `CacheSize` attribute and invoke the `add()` and `sayHello()` operations
 * *Login with monitorRole will not allow changing the cacheSize or to invoke the operations since it was given the readOnly privilege*
 * *controlRole allows changing the attribute values and to invoke operations successfully since it has the readWrite privilege*

    



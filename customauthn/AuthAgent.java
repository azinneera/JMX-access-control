package com.customauthn;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import javax.management.remote.JMXAuthenticator;
import javax.security.auth.Subject;
import java.security.Principal;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;


import java.lang.management.*;
import java.rmi.registry.*;
import java.util.*;
import javax.management.*;
import javax.management.remote.*;
import javax.management.remote.rmi.*;
import javax.rmi.ssl.*;
import java.util.HashMap;

public class AuthAgent {

    public static class RealmJMXAuthenticator implements JMXAuthenticator {

        public Subject authenticate(Object credentials) {

            // Verify that credentials is of type String[].
            //
            if (!(credentials instanceof String[])) {
                // Special case for null so we get a more informative message
                if (credentials == null) {
                    throw new SecurityException("Credentials required");
                }
                throw new SecurityException("Credentials should be String[]");
            }

            // Verify that the array contains three elements (username/password/realm).
            //
            final String[] aCredentials = (String[]) credentials;
            if (aCredentials.length != 2) {
                throw new SecurityException("Credentials should have 2 elements");
            }

            // Perform authentication
            //
            String username = (String) aCredentials[0];
            String password = (String) aCredentials[1];
//            String realm = (String) aCredentials[2];

//            ... perform authentication based on the (username/password/realm) tuple ...

            if (username.equals("user1") && password.equals("QED")|| username.equals("controlRole") && password.equals("R&D")) {
                return new Subject(true,
                        Collections.singleton(new JMXPrincipal(username)),
                        Collections.EMPTY_SET,
                        Collections.EMPTY_SET);
            } else {
                throw new SecurityException("Invalid credentials");
            }
        }
    }

    public static void main (String args[]) throws Exception {

        // Ensure cryptographically strong random number generator used
        // to choose the object number - see java.rmi.server.ObjID
        //
        System.setProperty("java.rmi.server.randomIDs", "true");

        // Start an RMI registry on port 3000.
        //
        System.out.println("Create RMI registry on port 3000");
        LocateRegistry.createRegistry(3000);

        // Retrieve the PlatformMBeanServer.
        //
        System.out.println("Get the platform's MBean server");
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

//   --------------------------------------------------------------------------

        // Construct the ObjectName for the Hello MBean we will register
        ObjectName mbeanName = new ObjectName("com.example:type=Hello");

        // Create the Hello World MBean
        Hello mbean = new Hello();

        // Register the Hello World MBean
        mbs.registerMBean(mbean, mbeanName);
//------------------------------------------------------------------------------

        // Environment map.
        //
        System.out.println("Initialize the environment map");
        HashMap<String,Object> env = new HashMap<String,Object>();

        // Provide SSL-based RMI socket factories.
        //
        // The protocol and cipher suites to be enabled will be the ones
        // defined by the default JSSE implementation and only server
        // authentication will be required.
        //
        SslRMIClientSocketFactory csf = new SslRMIClientSocketFactory();
        SslRMIServerSocketFactory ssf = new SslRMIServerSocketFactory();
        env.put(RMIConnectorServer.RMI_CLIENT_SOCKET_FACTORY_ATTRIBUTE, csf);
        env.put(RMIConnectorServer.RMI_SERVER_SOCKET_FACTORY_ATTRIBUTE, ssf);

        // Provide the password file used by the connector server to
        // perform user authentication. The password file is a properties
        // based text file specifying username/password pairs.
        //
        env.put(JMXConnectorServer.AUTHENTICATOR, new RealmJMXAuthenticator());

        // Provide the access level file used by the connector server to
        // perform user authorization. The access level file is a properties
        // based text file specifying username/access level pairs where
        // access level is either "readonly" or "readwrite" access to the
        // MBeanServer operations.
        //
//        env.put("jmx.remote.x.password.file", "jmxremote.password");
        env.put("jmx.remote.x.access.file", "jmxremote.access");

        // Create an RMI connector server.
        //
        // As specified in the JMXServiceURL the RMIServer stub will be
        // registered in the RMI registry running in the local host on
        // port 3000 with the name "jmxrmi". This is the same name the
        // out-of-the-box management agent uses to register the RMIServer
        // stub too.
        //
//        System.out.println("Create an RMI connector server");
//        JMXServiceURL url =
//                new JMXServiceURL("service:jmx:rmi:///jndi/rmi://:3000/jmxrmi");
//        JMXConnectorServer cs =
//                JMXConnectorServerFactory.newJMXConnectorServer(url, env, mbs);

//        MBeanServerForwarder mbsf = MBSFInvocationHandler.newProxyInstance();
//        cs.setMBeanServerForwarder(mbsf);

        // Start the RMI connector server.
        //
//        System.out.println("Start the RMI connector server");
//        cs.start();



        System.out.println("Create an RMI connector server");
        JMXServiceURL url =
                new JMXServiceURL("service:jmx:rmi:///jndi/rmi://:3000/jmxrmi");
        JMXConnectorServer cs =
                JMXConnectorServerFactory.newJMXConnectorServer(url, env, mbs);

        // Start the RMI connector server.
        //
        System.out.println("Start the RMI connector server");
        cs.start();

    }
}
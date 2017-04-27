package com.customauthz;

import java.io.IOException;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.security.AccessControlContext;
import java.security.AccessController;
import javax.security.auth.Subject;
import java.security.Principal;

import java.lang.management.*;
import java.rmi.registry.*;
import java.util.*;
import javax.management.*;
import javax.management.remote.*;
import javax.management.remote.rmi.*;
import javax.rmi.ssl.*;
import java.util.HashMap;

public class Agent {

    public static class MBSFInvocationHandler implements InvocationHandler {

        public static MBeanServerForwarder newProxyInstance() {

            final InvocationHandler handler = new MBSFInvocationHandler();

            final Class[] interfaces =
                    new Class[] {MBeanServerForwarder.class};

            Object proxy = Proxy.newProxyInstance(
                    MBeanServerForwarder.class.getClassLoader(),
                    interfaces,
                    handler);

            return MBeanServerForwarder.class.cast(proxy);
        }

        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

            final String methodName = method.getName();

            if (methodName.equals("getMBeanServer")) {
                return mbs;
            }

            if (methodName.equals("setMBeanServer")) {
                if (args[0] == null)
                    throw new IllegalArgumentException("Null MBeanServer");
                if (mbs != null)
                    throw new IllegalArgumentException("MBeanServer object " +
                            "already initialized");
                mbs = (MBeanServer) args[0];
                return null;
            }

            // Retrieve Subject from current AccessControlContext
            AccessControlContext acc = AccessController.getContext();
            Subject subject = Subject.getSubject(acc);

            // Allow operations performed locally on behalf of the connector server itself
            if (subject == null) {
                return method.invoke(mbs, args);
            }

            // Restrict access to "createMBean" and "unregisterMBean" to any user
            if (methodName.equals("createMBean") || methodName.equals("unregisterMBean")) {
                throw new SecurityException("Access denied");
            }

            // Retrieve JMXPrincipal from Subject
            Set<JMXPrincipal> principals = subject.getPrincipals(JMXPrincipal.class);
            if (principals == null || principals.isEmpty()) {
                throw new SecurityException("Access denied");
            }
            Principal principal = principals.iterator().next();
            String identity = principal.getName();

            // "user2" can perform any operation other than "createMBean" and "unregisterMBean"
            if (identity.equals("user2")) {
                return method.invoke(mbs, args);
            }

            // "usera" cannot call "setAttribute" and "sayHello" operation on the MBean Server
            if (identity.equals("user1") &&
                    !(methodName.equals("setAttribute") || (methodName.equals("invoke") && args.length>1 && args[1].equals("sayHello")))) {
                /*if(args.length>1) {
                    System.out.println(args[1].toString());
                }*/
                return method.invoke(mbs, args);
            }

            throw new SecurityException("Access denied");
        }

        private MBeanServer mbs;
    }


    public static void main(String[] args) throws Exception {

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
//        env.put(JMXConnectorServer.AUTHENTICATOR, new RealmJMXAuthenticator());

        // Provide the access level file used by the connector server to
        // perform user authorization. The access level file is a properties
        // based text file specifying username/access level pairs where
        // access level is either "readonly" or "readwrite" access to the
        // MBeanServer operations.
        //
        env.put("jmx.remote.x.password.file", "jmxremote.password");

        // Create an RMI connector server.
        //
        // As specified in the JMXServiceURL the RMIServer stub will be
        // registered in the RMI registry running in the local host on
        // port 3000 with the name "jmxrmi". This is the same name the
        // out-of-the-box management agent uses to register the RMIServer
        // stub too.
        //
        System.out.println("Create an RMI connector server");
        JMXServiceURL url =
                new JMXServiceURL("service:jmx:rmi:///jndi/rmi://:3000/jmxrmi");
        JMXConnectorServer cs =
                JMXConnectorServerFactory.newJMXConnectorServer(url, env, mbs);

        MBeanServerForwarder mbsf = MBSFInvocationHandler.newProxyInstance();
        cs.setMBeanServerForwarder(mbsf);

        // Start the RMI connector server.
        //
        System.out.println("Start the RMI connector server");
        cs.start();
    }
}
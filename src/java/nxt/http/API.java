package nxt.http;

import com.codahale.metrics.jetty9.InstrumentedHandler;
import nxt.Constants;
import nxt.Nxt;
import nxt.util.Subnet;
import nxt.util.ThreadPool;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.eclipse.jetty.servlets.GzipFilter;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class API {

    static final Set<Subnet> allowedBotHosts;
    static final boolean enableDebugAPI = Nxt.getBooleanProperty("nxt.enableDebugAPI");
    private static final Logger logger = LoggerFactory.getLogger(API.class);
    private static final int TESTNET_API_PORT = 6876;
    private static final Server apiServer;

    static {
        List<String> allowedBotHostsList = Nxt.getStringListProperty("nxt.allowedBotHosts");
        if (!allowedBotHostsList.contains("*")) {
            // Temp hashset to store allowed subnets
            Set<Subnet> allowedSubnets = new HashSet<>();

            for (String allowedHost : allowedBotHostsList) {
                try {
                    allowedSubnets.add(Subnet.createInstance(allowedHost));
                } catch (UnknownHostException e) {
                    logger.error("Error adding allowed bot host '" + allowedHost + "'", e);
                }
            }
            allowedBotHosts = Collections.unmodifiableSet(allowedSubnets);
        } else {
            allowedBotHosts = null;
        }

        boolean enableAPIServer = Nxt.getBooleanProperty("nxt.enableAPIServer");
        if (enableAPIServer) {
            final int port = Constants.isTestnet ? TESTNET_API_PORT : Nxt.getIntProperty("nxt.apiServerPort");
            final String host = Nxt.getStringProperty("nxt.apiServerHost");
            apiServer = new Server();
            ServerConnector connector;

            boolean enableSSL = Nxt.getBooleanProperty("nxt.apiSSL");
            if (enableSSL) {
                logger.info("Using SSL (https) for the API server");
                HttpConfiguration https_config = new HttpConfiguration();
                https_config.setSecureScheme("https");
                https_config.setSecurePort(port);
                https_config.addCustomizer(new SecureRequestCustomizer());
                SslContextFactory sslContextFactory = new SslContextFactory();
                sslContextFactory.setKeyStorePath(Nxt.getStringProperty("nxt.keyStorePath"));
                sslContextFactory.setKeyStorePassword(Nxt.getStringProperty("nxt.keyStorePassword"));
                sslContextFactory.setExcludeCipherSuites("SSL_RSA_WITH_DES_CBC_SHA", "SSL_DHE_RSA_WITH_DES_CBC_SHA",
                        "SSL_DHE_DSS_WITH_DES_CBC_SHA", "SSL_RSA_EXPORT_WITH_RC4_40_MD5", "SSL_RSA_EXPORT_WITH_DES40_CBC_SHA",
                        "SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA", "SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA");
                sslContextFactory.setExcludeProtocols("SSLv3");
                connector = new ServerConnector(apiServer, new SslConnectionFactory(sslContextFactory, "http/1.1"),
                        new HttpConnectionFactory(https_config));
            } else {
                connector = new ServerConnector(apiServer);
            }

            connector.setPort(port);
            connector.setHost(host);
            connector.setIdleTimeout(Nxt.getIntProperty("nxt.apiServerIdleTimeout"));
            connector.setReuseAddress(true);
            apiServer.addConnector(connector);

            HandlerList apiHandlers = new HandlerList();

            ServletContextHandler apiHandler = new ServletContextHandler();
            String apiResourceBase = Nxt.getStringProperty("nxt.apiResourceBase");
            if (apiResourceBase != null) {
                ServletHolder defaultServletHolder = new ServletHolder(new DefaultServlet());
                defaultServletHolder.setInitParameter("dirAllowed", "false");
                defaultServletHolder.setInitParameter("resourceBase", apiResourceBase);
                defaultServletHolder.setInitParameter("welcomeServlets", "true");
                defaultServletHolder.setInitParameter("redirectWelcome", "true");
                defaultServletHolder.setInitParameter("gzip", "true");
                apiHandler.addServlet(defaultServletHolder, "/*");
                apiHandler.setWelcomeFiles(new String[]{"index.html"});
            }

            String javadocResourceBase = Nxt.getStringProperty("nxt.javadocResourceBase");
            if (javadocResourceBase != null) {
                ContextHandler contextHandler = new ContextHandler("/doc");
                ResourceHandler docFileHandler = new ResourceHandler();
                docFileHandler.setDirectoriesListed(false);
                docFileHandler.setWelcomeFiles(new String[]{"index.html"});
                docFileHandler.setResourceBase(javadocResourceBase);
                contextHandler.setHandler(docFileHandler);
                apiHandlers.addHandler(contextHandler);
            }

            apiHandler.addServlet(APIServlet.class, "/burst");
            if (Nxt.getBooleanProperty("nxt.enableAPIServerGZIPFilter")) {
                FilterHolder gzipFilterHolder = apiHandler.addFilter(GzipFilter.class, "/burst", null);
                gzipFilterHolder.setInitParameter("methods", "GET,POST");
                gzipFilterHolder.setAsyncSupported(true);
            }

            apiHandler.addServlet(APITestServlet.class, "/test");

            if (Nxt.getBooleanProperty("nxt.apiServerCORS")) {
                FilterHolder filterHolder = apiHandler.addFilter(CrossOriginFilter.class, "/*", null);
                filterHolder.setInitParameter("allowedHeaders", "*");
                filterHolder.setAsyncSupported(true);
            }

            InstrumentedHandler instrumentedApiHandler = new InstrumentedHandler(Nxt.metrics, "api-handler");
            instrumentedApiHandler.setHandler(apiHandler);
            apiHandlers.addHandler(instrumentedApiHandler);
            apiHandlers.addHandler(new DefaultHandler());

            apiServer.setHandler(apiHandlers);
            apiServer.setStopAtShutdown(true);

            ThreadPool.runBeforeStart(new Runnable() {
                @Override
                public void run() {
                    try {
                        apiServer.start();
                        logger.info("Started API server at " + host + ":" + port);
                    } catch (Exception e) {
                        logger.error("Failed to start API server", e);
                        throw new RuntimeException(e.toString(), e);
                    }

                }
            }, true);

        } else {
            apiServer = null;
            logger.info("API server not enabled");
        }

    }

    private API() {
    } // never

    public static void init() {
    }

    public static void shutdown() {
        if (apiServer != null) {
            try {
                apiServer.stop();
            } catch (Exception e) {
                logger.info("Failed to stop API server", e);
            }
        }
    }

}

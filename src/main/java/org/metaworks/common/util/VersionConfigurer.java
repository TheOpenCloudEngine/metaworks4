package org.metaworks.common.util;

import org.apache.commons.io.IOUtils;
import org.metaworks.common.logging.WebLogbackConfigurer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Application Version을 표시하는 Configurer.
 *
 * @author Seungpil, Park
 * @since 0.1
 */
public class VersionConfigurer implements javax.servlet.ServletContextListener {

    /**
     * SLF4J Logging
     */
    private Logger logger = LoggerFactory.getLogger(VersionConfigurer.class);

    private static final long MEGA_BYTES = 1024 * 1024;

    private static final String UNKNOWN = "Unknown";

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        System.setProperty("PID", SystemUtils.getPid());

        Properties properties = new Properties();
        ServletContext context = servletContextEvent.getServletContext();
        InputStream inputStream = null;
        try {
            URL applicationUrl = getClass().getResource("/application.properties");
            File applicationFile = ResourceUtils.getFile(applicationUrl);
            inputStream = new FileInputStream(applicationFile);// context.getResourceAsStream("application.properties");
            properties.load(inputStream);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Cannot load a 'application.properties' file.", ex);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }

        StringBuilder builder = new StringBuilder();
        builder.append("   __    __ _______  ____    __  _____      __ ____    __ _______        \n" +
                       "  / /   / // _____/ / _  |  / / / ___ \\    / // _  |  / // _____/       \n" +
                       " / /   / // /_____ / / | | / / / /  /_/   / // / | | / // /_____          \n" +
                       "/ /___/ //  _____// /  | |/ / / /_/ / \\  / // /  | |/ //  _____/          \n" +
                       "\\_____/_/\\______//_/   |___/  \\___,__/_ /_//_/   |___/ \\______/             \n" +
                       "                                                           ");

        printHeader(builder, "Application Information");
        Properties appProps = new Properties();
        appProps.put("Instance", StringUtils.isEmpty(System.getProperty("instance")) ? "** UNKNOWN **" : System.getProperty("instance"));
        appProps.put("Application", properties.get("name"));
        appProps.put("Version", properties.get("version"));
        appProps.put("Build Date", properties.get("build.timestamp"));
        appProps.put("Build Number", properties.get("build.number"));
        appProps.put("Revision Number", properties.get("revision.number"));
        appProps.put("Copyright", properties.get("copyright"));
        appProps.put("Organization", properties.get("organization"));
        appProps.put("Developers", properties.get("developers"));

        if (context != null) {
            appProps.put("Application Server", context.getServerInfo() + " - Servlet API " + context.getMajorVersion() + "." + context.getMinorVersion());
        }

        Properties systemProperties = System.getProperties();
        appProps.put("Java Version", systemProperties.getProperty("java.version", UNKNOWN) + " - " + systemProperties.getProperty("java.vendor", UNKNOWN));
        appProps.put("Current Working Directory", systemProperties.getProperty("user.dir", UNKNOWN));

        print(builder, appProps);

        Properties memPros = new Properties();
        final Runtime rt = Runtime.getRuntime();
        final long maxMemory = rt.maxMemory() / MEGA_BYTES;
        final long totalMemory = rt.totalMemory() / MEGA_BYTES;
        final long freeMemory = rt.freeMemory() / MEGA_BYTES;
        final long usedMemory = totalMemory - freeMemory;

        memPros.put("Maximum Allowable Memory", maxMemory + "MB");
        memPros.put("Total Memory", totalMemory + "MB");
        memPros.put("Free Memory", freeMemory + "MB");
        memPros.put("Used Memory", usedMemory + "MB");

        print(builder, memPros);

        printHeader(builder, "Java System Properties");
        Properties sysProps = new Properties();
        for (final Map.Entry<Object, Object> entry : systemProperties.entrySet()) {
            sysProps.put(entry.getKey(), entry.getValue());
        }

        print(builder, sysProps);

        printHeader(builder, "System Environments");
        Map<String, String> getenv = System.getenv();
        Properties envProps = new Properties();
        Set<String> strings = getenv.keySet();
        for (String key : strings) {
            String message = getenv.get(key);
            envProps.put(key, message);
        }

        print(builder, envProps);

        WebLogbackConfigurer.initLogging(servletContextEvent.getServletContext());

        System.out.println(builder.toString());

        logger.info("============================================================");
        logger.info(" " + properties.get("name") + " (" + SystemUtils.getPid() + ") starting...");
        logger.info("============================================================");
    }

    private void printHeader(StringBuilder builder, String message) {
        builder.append(org.slf4j.helpers.MessageFormatter.format("\n== {} =====================\n", message).getMessage()).append("\n");
    }

    private void print(StringBuilder builder, Properties props) {
        int maxLength = getMaxLength(props);
        Enumeration<Object> keys = props.keys();
        while (keys.hasMoreElements()) {
            String key = (String) keys.nextElement();
            String value = props.getProperty(key);
            builder.append("  ").append(key).append(getCharacter(maxLength - key.getBytes().length, " ")).append(" : ").append(value).append("\n");
        }
    }

    private int getMaxLength(Properties props) {
        Enumeration<Object> keys = props.keys();
        int maxLength = -1;
        while (keys.hasMoreElements()) {
            String key = (String) keys.nextElement();
            if (maxLength < 0) {
                maxLength = key.getBytes().length;
            } else if (maxLength < key.getBytes().length) {
                maxLength = key.getBytes().length;
            }
        }
        return maxLength;
    }

    /**
     * 지정한 크기 만큼 문자열을 구성한다.
     *
     * @param size      문자열을 구성할 반복수
     * @param character 문자열을 구성하기 위한 단위 문자열. 반복수만큼 생성된다.
     * @return 문자열
     */
    private static String getCharacter(int size, String character) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < size; i++) {
            builder.append(character);
        }
        return builder.toString();
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        WebLogbackConfigurer.shutdownLogging(servletContextEvent.getServletContext());
    }
}
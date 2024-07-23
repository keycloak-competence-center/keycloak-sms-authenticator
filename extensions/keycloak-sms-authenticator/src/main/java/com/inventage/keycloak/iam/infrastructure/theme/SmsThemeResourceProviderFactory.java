package com.inventage.keycloak.iam.infrastructure.theme;

import org.jboss.logging.Logger;
import org.keycloak.theme.ClasspathThemeResourceProviderFactory;
import org.keycloak.theme.ThemeResourceProvider;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Locale;
import java.util.Properties;

/**
 * The {@link SmsThemeResourceProviderFactory} ensures that all custom themes, templates and messages are loaded during runtime.
 */
public class SmsThemeResourceProviderFactory extends ClasspathThemeResourceProviderFactory {

    private static final Logger LOGGER = Logger.getLogger(SmsThemeResourceProviderFactory.class);
    private static final String ID = "sms-classpath";

    private final ThemeResourceProvider resourceProviderFactory = new ClasspathThemeResourceProviderFactory("sms-fallback-classpath", ClasspathThemeResourceProviderFactory.class.getClassLoader());

    public SmsThemeResourceProviderFactory() {
        super(ID, SmsThemeResourceProviderFactory.class.getClassLoader());
    }

    @Override
    public URL getTemplate(String name) throws IOException {
        final URL template = super.getTemplate(name);
        if (template != null) {
            return template;
        }
        LOGGER.debugf("getTemplate: for name '%s' via fallback", name);
        return resourceProviderFactory.getTemplate(name);
    }

    @Override
    public InputStream getResourceAsStream(String path) throws IOException {
        final InputStream resourceAsStream = super.getResourceAsStream(path);
        if (resourceAsStream != null) {
            return resourceAsStream;
        }
        LOGGER.debugf("getResourceAsStream: for path '%s' via fallback", path);
        return resourceProviderFactory.getResourceAsStream(path);
    }

    @Override
    public Properties getMessages(String baseBundlename, Locale locale) throws IOException {
        LOGGER.debugf("getMessages: for baseBundlename '%s'", baseBundlename);
        final Properties fallbackMessages = resourceProviderFactory.getMessages(baseBundlename, locale);
        final Properties messages = super.getMessages(baseBundlename, locale);
        if (messages != null) {
            fallbackMessages.putAll(messages);
        }
        return fallbackMessages;
    }

}

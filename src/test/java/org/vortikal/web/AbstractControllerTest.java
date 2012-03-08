package org.vortikal.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import junit.framework.TestCase;

import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.vortikal.context.BaseContext;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.Path;
import org.vortikal.repository.Repository;
import org.vortikal.repository.resourcetype.PropertyType.Type;
import org.vortikal.repository.resourcetype.PropertyTypeDefinitionImpl;
import org.vortikal.repository.resourcetype.ValueFactoryImpl;
import org.vortikal.repository.resourcetype.ValueFormatter;
import org.vortikal.security.SecurityContext;

public abstract class AbstractControllerTest extends TestCase {

    static {
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.Log4JLogger");
        System.setProperty("log4j.configuration", "log4j.test.xml");
    }

    protected Mockery context = new JUnit4Mockery();
    protected final HttpServletRequest mockRequest = context.mock(HttpServletRequest.class);
    protected final HttpServletResponse mockResponse = context.mock(HttpServletResponse.class);
    protected final Repository mockRepository = context.mock(Repository.class);

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        BaseContext.pushContext();
        SecurityContext securityContext = new SecurityContext(null, null);
        SecurityContext.setSecurityContext(securityContext);
        RequestContext requestContext = new RequestContext(null, securityContext, null, null, getRequestPath(), null, false, false, true, mockRepository);
        RequestContext.setRequestContext(requestContext);
    }

    protected abstract Path getRequestPath();

    protected PropertyTypeDefinitionImpl getPropDef(Namespace namespace, String name, Type type,
            ValueFormatter valueFormatter) {
        PropertyTypeDefinitionImpl propDef = new PropertyTypeDefinitionImpl();
        propDef.setValueFactory(new ValueFactoryImpl());
        propDef.setValueFormatter(valueFormatter);
        propDef.setType(type);
        propDef.setNamespace(namespace);
        propDef.setName(name);
        return propDef;
    }

}

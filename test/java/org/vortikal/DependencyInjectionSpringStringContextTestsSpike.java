package org.vortikal;



public class DependencyInjectionSpringStringContextTestsSpike extends AbstractDependencyInjectionSpringStringContextTests {


    protected String  getConfigAsString() {
        return("<?xml version=\"1.0\" encoding=\"UTF-8\"?> <!DOCTYPE beans PUBLIC \"-//SPRING//DTD BEAN//EN\" \"http://www.springframework.org /dtd/spring-beans.dtd\"><beans> <bean abstract=\"true\" id=\"test\"/></beans>");
    }
    
    public void testSpike() {
        assertTrue(true);
    }
}

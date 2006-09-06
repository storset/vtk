package org.vortikal.web.controller.repository.copy;

import java.io.InputStream;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.vortikal.repository.Repository;
import org.vortikal.security.SecurityContext;

public class SimpleFilteringCopyAction implements CopyAction, InitializingBean {

    private Repository repository;
    private Filter filter;

    public void process(String originalUri, String copyUri) throws Exception{
        String token = SecurityContext.getSecurityContext().getToken();

        InputStream is = this.repository.getInputStream(token, originalUri, true);

        this.repository.copy(token, originalUri, copyUri, "0", false, true);
        if (this.filter != null) {
            is = this.filter.transform(is);
            this.repository.storeContent(token, copyUri, is);
        } else {
            is.close();
        }
    }

    public void setFilter(Filter filter) {
        this.filter = filter;
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public void afterPropertiesSet() throws Exception {
        if (this.repository == null)
            throw new BeanInitializationException(
                    "Required Java Bean Property 'repository' not set");
    }

}

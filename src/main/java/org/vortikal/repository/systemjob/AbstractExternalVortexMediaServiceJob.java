package org.vortikal.repository.systemjob;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Required;

public abstract class AbstractExternalVortexMediaServiceJob extends RepositoryJob {

    protected PathSelector pathSelector;
    
    protected final Log logger = LogFactory.getLog(getClass());

    protected boolean continueOnException = true;

    protected String service;
    protected String repositoryDataDirectory;
    protected Map<String, String> serviceParameters;

    @Required
    public void setService(String service) {
        this.service = service;
    }

    @Required
    public void setRepositoryDataDirectory(String repositoryDataDirectory) {
        this.repositoryDataDirectory = repositoryDataDirectory;
    }

    @Required
    public void setServiceParameters(Map<String, String> serviceParameters) {
        this.serviceParameters = serviceParameters;
    }

    @Required
    public void setPathSelector(PathSelector pathSelector) {
        this.pathSelector = pathSelector;
    }

    public void setContinueOnException(boolean continueOnException) {
        this.continueOnException = continueOnException;
    }

}

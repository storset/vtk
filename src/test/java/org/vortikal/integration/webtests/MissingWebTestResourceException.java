package org.vortikal.integration.webtests;

import junit.framework.AssertionFailedError;

public class MissingWebTestResourceException extends AssertionFailedError {

    private static final long serialVersionUID = -3748879504495152268L;
    
    public MissingWebTestResourceException(String message) {
        super(message);
    }

}

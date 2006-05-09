package org.vortikal.repositoryimpl.query;

/**
 * Exception thrown when unable to decode an encoded index field value.
 * 
 * @author oyviste
 *
 */
public class FieldValueEncodingException extends FieldMappingException {

    public FieldValueEncodingException() {
        super();
        // TODO Auto-generated constructor stub
    }

    public FieldValueEncodingException(String message) {
        super(message);
        // TODO Auto-generated constructor stub
    }

    public FieldValueEncodingException(String message, Throwable cause) {
        super(message, cause);
        // TODO Auto-generated constructor stub
    }

    public FieldValueEncodingException(Throwable cause) {
        super(cause);
        // TODO Auto-generated constructor stub
    }

}

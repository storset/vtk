package org.vortikal.webdav.ifheader;

/**
 * The <code>IfListEntry</code> abstract class is the base class for
 * entries in an <em>IfList</em> production. This abstract base class
 * provides common functionality to both types of entries, namely tokens
 * enclosed in angle brackets (<code>&lt; &gt;</code>) and etags enclosed
 * in square brackets (<code>[ ]</code>).
 */
abstract class IfListEntry {

    /**
     * The entry string value - the semantics of this value depends on the
     * implementing class.
     */
    protected final String value;

    /** Flag to indicate, whether this is a positive match or not */
    protected final boolean positive;

    /** The cached result of the {@link #toString} method. */
    protected String stringValue;

    /**
     * Sets up the final fields of this abstract class. The meaning of
     * value parameter depends solely on the implementing class. From the
     * point of view of this abstract class, it is simply a string value.
     *
     * @param value The string value of this instance
     * @param positive <code>true</code> if matches are positive
     */
    
    protected boolean isEtag;
    
    protected IfListEntry(String value, boolean positive) {
        this.value = value;
        this.positive = positive;
    }

    public boolean isEtag() {
        return isEtag;
    }
    
    
    /**
     * Matches the value from the parameter to the internal string value.
     * If the parameter and the {@link #value} field match, the method
     * returns <code>true</code> for positive matches and <code>false</code>
     * for negative matches.
     * <p>
     * This helper method can be called by implementations to evaluate the
     * concrete match on the correct value parameter. See
     * {@link #match(String, String)} for the external API method.
     *
     * @param value The string value to compare to the {@link #value}
     *      field.
     *
     * @return <code>true</code> if the value parameter and the
     *      {@link #value} field match and the {@link #positive} field is
     *      <code>true</code> or if the values do not match and the
     *      {@link #positive} field is <code>false</code>.
     */
    protected boolean match(String value) {
        return positive == this.value.equals(value);
    }

    /**
     * Matches the entry's value to the the token or etag. Depending on the
     * concrete implementation, only one of the parameters may be evaluated
     * while the other may be ignored.
     * <p>
     * Implementing METHODS may call the helper method {@link #match(String)}
     * for the actual matching.
     *
     * @param token The token value to compare
     * @param etag The etag value to compare
     *
     * @return <code>true</code> if the token/etag matches the <em>IfList</em>
     *      entry.
     */
    public abstract boolean match(String token, String etag);

     
    /**
     * Returns a short type name for the implementation. This method is
     * used by the {@link #toString} method to build the string representation
     * if the instance.
     *
     * @return The type name of the implementation.
     */
    protected abstract String getType();

/**
 * Returns the value of this entry.
 *
 * @return the value
 */
protected String getValue() {
    return value;
}

    /**
     * Returns the String represenation of this entry. This method uses the
     * {@link #getType} to build the string representation.
     *
     * @return the String represenation of this entry.
     */
    public String toString() {
        if (stringValue == null) {
            stringValue = getType() + ": " + (positive?"":"!") + value;
        }
        return stringValue;
    }
}
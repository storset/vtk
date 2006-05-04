package org.vortikal.webdav.ifheader;


/**
 * The <code>IfListEntryToken</code> extends the {@link IfListEntry}
 * abstract class to represent an entry for token matching.
 */
 class IfListEntryToken extends IfListEntry {

    /**
     * Creates a token matching entry.
     *
     * @param token The token value pertinent to this instance.
     * @param positive <code>true</code> if this is a positive match entry.
     */
    IfListEntryToken(String token, boolean positive) {
        super(token, positive);
        isEtag = false;
    }

    /**
     * Matches the token parameter to the stored token value and returns
     * <code>true</code> if the values match and if the match is positive.
     * <code>true</code> is also returned for negative matches if the values
     * do not match.
     *
     * @param token The token value to compare
     * @param etag The etag value to compare, which is ignored in this
     *      implementation.
     *
     * @return <code>true</code> if the token matches the <em>IfList</em>
     *      entry's token value.
     */
    public boolean match(String token, String etag) {
        return super.match(token);
    }

    
    /**
     * Returns the type name of this implementation, which is fixed to
     * be <em>Token</em>.
     *
     * @return The fixed string <em>Token</em> as the type name.
     */
    protected String getType() {
        return "Token";
    }

}
package org.vortikal.webdav.ifheader;


/**
 * The <code>IfListEntryToken</code> extends the {@link IfListEntry}
 * abstract class to represent an entry for etag matching.
 */
class IfListEntryEtag extends IfListEntry {

    /**
     * Creates an etag matching entry.
     *
     * @param etag The etag value pertinent to this instance.
     * @param positive <code>true</code> if this is a positive match entry.
     */
    IfListEntryEtag(String etag, boolean positive) {
        super(etag, positive);
        isEtag = true;
    }

    /**
     * Matches the etag parameter to the stored etag value and returns
     * <code>true</code> if the values match and if the match is positive.
     * <code>true</code> is also returned for negative matches if the values
     * do not match.
     *
     * @param token The token value to compare, which is ignored in this
     *      implementation.
     * @param etag The etag value to compare
     *
     * @return <code>true</code> if the etag matches the <em>IfList</em>
     *      entry's etag value.
     */
    public boolean match(String token, String etag) {
        return super.match(etag);
    }
    
    /**
     * Returns the type name of this implementation, which is fixed to
     * be <em>ETag</em>.
     *
     * @return The fixed string <em>ETag</em> as the type name.
     */
    protected String getType() {
        return "ETag";
    }


}
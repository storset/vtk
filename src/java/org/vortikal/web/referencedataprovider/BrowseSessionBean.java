package org.vortikal.web.referencedataprovider;


/**
 * Instances of this class stores information about which field in the
 * edited document to place the generated URL from the browse app.
 *
 * @author Even Halvorsen
 * @author Tomm Eriksen
 * @version $Id: BrowseSessionBean.java,v 1.4 2004/02/26 10:33:51 gormap Exp $
 *
 */

public class BrowseSessionBean {

   private String editField = "";
   private String startUrl = "";


   public String getEditField() {
      return this.editField;
   }
    
   public void setEditField(String editField) {
      this.editField= editField;
   }

   public String getStartUrl() {
      return this.startUrl;
   }
    
   public void setStartUrl(String startUrl) {
      this.startUrl= startUrl;
   }
}

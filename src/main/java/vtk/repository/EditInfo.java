package vtk.repository;

import vtk.security.Principal;

public class EditInfo {
    private boolean isEditAuthorized = false;
    private boolean isEditLocked = false;
    private Principal lockedBy;
    
    public EditInfo() {
    }
    
    public EditInfo(boolean isEditAuthorized, boolean isEditLocked, Principal lockedBy) {
        this.isEditAuthorized = isEditAuthorized;
        this.isEditLocked = isEditLocked;
        this.lockedBy = lockedBy;
    }
    
    public boolean isEditAuthorized() {
        return isEditAuthorized;
    }
    
    public boolean isEditLocked() {
        return isEditLocked;
    }
    
    public Principal getLockedBy() {
        return lockedBy;
    }

    public String getLockedByNameHref() {
        if (lockedBy != null) {
            String lockedByName = lockedBy.getName();
            String url = lockedBy.getURL();
            if (url != null) {
                lockedByName = "<a href=\"" + lockedBy.getURL() + "\">" + lockedBy.getDescription() + "</a>";
            }
            return lockedByName;
        }
        return "";
    }
}

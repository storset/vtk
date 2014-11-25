package vtk.repository;

public class EditInfo {
    private boolean isEditAuthorized = false;
    private boolean isEditLocked = false;
    private String lockedBy = "";
    
    public EditInfo() {
    }
    
    public EditInfo(boolean isEditAuthorized, boolean isEditLocked, String lockedBy) {
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
    
    public String getLockedBy() {
        return lockedBy;
    }

    public void setLockedBy(String lockedBy) {
        this.lockedBy = lockedBy;
    }

    public void setEditAuthorized(boolean isEditAuthorized) {
        this.isEditAuthorized = isEditAuthorized;
    }

    public void setEditLocked(boolean isEditLocked) {
        this.isEditLocked = isEditLocked;
    }
}

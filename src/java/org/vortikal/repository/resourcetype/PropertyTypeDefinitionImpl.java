package org.vortikal.repository.resourcetype;

import org.vortikal.repository.AuthorizationException;
import org.vortikal.repository.NuResource;
import org.vortikal.repository.Repository;
import org.vortikal.repository.RepositoryOperations;
import org.vortikal.repositoryimpl.Resource;
import org.vortikal.security.Principal;
import org.vortikal.security.roles.RoleManager;

public class PropertyTypeDefinitionImpl implements PropertyTypeDefinition {

    // XXX: Default values?
    private String name;
    private int type = PropertyType.TYPE_STRING;
    private boolean isMultiple = false;
    private int protectionLevel = PropertyType.PROTECTION_LEVEL_EDITABLE;
    private boolean isMandatory = false; // Is this interesting?
    private Constraint constraint;
    private PropertyEvaluator propertyEvaluator;

    public Constraint getConstraint() {
        return constraint;
    }

    public void setConstraint(Constraint constraint) {
        this.constraint = constraint;
    }

    public boolean isMandatory() {
        return isMandatory;
    }

    public void setMandatory(boolean isMandatory) {
        this.isMandatory = isMandatory;
    }

    public boolean isMultiple() {
        return isMultiple;
    }

    public void setMultiple(boolean isMultiple) {
        this.isMultiple = isMultiple;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public PropertyEvaluator getPropertyEvaluator() {
        return propertyEvaluator;
    }

    public void setPropertyEvaluator(PropertyEvaluator propertyEvaluator) {
        this.propertyEvaluator = propertyEvaluator;
    }

    public int getProtectionLevel() {
        return protectionLevel;
    }

    public void setProtectionLevel(int protectionLevel) {
        this.protectionLevel = protectionLevel;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }
    
}

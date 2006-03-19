package org.vortikal.repository.resourcetype;


public class PropertyTypeDefinitionImpl implements PropertyTypeDefinition {

    // XXX: Default values?
    private String name;
    private int type = PropertyType.TYPE_STRING;
    private boolean multiple = false;
    private int protectionLevel = PropertyType.PROTECTION_LEVEL_EDITABLE;
    private boolean mandatory = false; // Is this interesting?
    private Constraint constraint;
    private CreatePropertyEvaluator createEvaluator;
    private ContentModificationPropertyEvaluator contentModificationEvaluator;
    private PropertiesModificationPropertyEvaluator propertiesModificationEvaluator;
    private PropertyValidator validator;
    
    public ContentModificationPropertyEvaluator getContentModificationEvaluator() {
        return contentModificationEvaluator;
    }

    public void setContentModificationEvaluator(
            ContentModificationPropertyEvaluator contentModificationEvaluator) {
        this.contentModificationEvaluator = contentModificationEvaluator;
    }

    public CreatePropertyEvaluator getCreateEvaluator() {
        return createEvaluator;
    }

    public void setCreateEvaluator(CreatePropertyEvaluator createEvaluator) {
        this.createEvaluator = createEvaluator;
    }

    public PropertiesModificationPropertyEvaluator getPropertiesModificationEvaluator() {
        return propertiesModificationEvaluator;
    }

    public void setPropertiesModificationEvaluator(
            PropertiesModificationPropertyEvaluator propertiesModificationEvaluator) {
        this.propertiesModificationEvaluator = propertiesModificationEvaluator;
    }

    public Constraint getConstraint() {
        return constraint;
    }

    public void setConstraint(Constraint constraint) {
        this.constraint = constraint;
    }

    public boolean isMandatory() {
        return this.mandatory;
    }

    public void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;
    }

    public boolean isMultiple() {
        return multiple;
    }

    public void setMultiple(boolean multiple) {
        this.multiple = multiple;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    /**
     * @return Returns the validator.
     */
    public PropertyValidator getValidator() {
        return validator;
    }

    /**
     * @param validator The validator to set.
     */
    public void setValidator(PropertyValidator validator) {
        this.validator = validator;
    }
    
}

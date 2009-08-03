package org.vortikal.resourcemanagement;

public class ScriptDefinition {
    
    public enum ScriptType {SHOWHIDE, AUTOCOMPLETE};
    
    private String name;
    private ScriptType type;
    private Object params;
    
    public ScriptDefinition(String name, ScriptType type, Object params) {
        this.name = name;
        this.type = type;
        this.params = params;
    }
    
    public String getName() {
        return this.name;
    }
    
    public ScriptType getType() {
        return this.type;
    }
    
    public Object getParams() {
        return this.params;
    }

}

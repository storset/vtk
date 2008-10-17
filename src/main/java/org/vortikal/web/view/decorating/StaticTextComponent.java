package org.vortikal.web.view.decorating;

import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

public class StaticTextComponent implements DecoratorComponent {
    
    private StringBuilder content;

    public StaticTextComponent(StringBuilder content) {
        this.content = content;
    }
    
    public void render(DecoratorRequest request, DecoratorResponse response)
        throws Exception {
        
        Writer out = response.getWriter();
        out.write(this.content.toString());
        out.close();
    }


    public String getNamespace() {
        return null;
    }

    public String getName() {    
        return "StaticText";
    }

    public String getDescription() {
        return "";
    }

    public Map<String, String> getParameterDescriptions() {
        return new HashMap<String, String>();
    }

    public String toString() {
        return getName();
    }

    public StringBuilder getBuffer() {
        return this.content;
    }
    
}

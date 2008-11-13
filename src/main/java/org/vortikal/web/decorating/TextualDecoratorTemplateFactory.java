package org.vortikal.web.decorating;

import org.springframework.beans.factory.annotation.Required;

public class TextualDecoratorTemplateFactory implements TemplateFactory {

    private TextualComponentParser parser;

    public Template newTemplate(TemplateSource templateSource) throws InvalidTemplateException {
        return new TextualDecoratorTemplate(this.parser, templateSource);
    }

    @Required public void setParser(TextualComponentParser parser) {
        this.parser = parser;
    }
}

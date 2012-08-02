package org.vortikal.repository.resourcetype.property;

import org.vortikal.repository.Property;
import org.vortikal.repository.PropertyEvaluationContext;
import org.vortikal.repository.PropertyEvaluationContext.Type;
import org.vortikal.repository.resourcetype.LatePropertyEvaluator;
import org.vortikal.repository.resourcetype.Value;

public class ThumbnailStatusPropertyEvaluator implements LatePropertyEvaluator {

    @Override
    public boolean evaluate(Property property, PropertyEvaluationContext ctx) throws PropertyEvaluationException {

        if (property.isValueInitialized() && ctx.getEvaluationType() != Type.ContentChange
                && ctx.getEvaluationType() != Type.Create) {
            return true;
        }

        property.setValues(new Value[] { new Value("GENERATE_TUMBNAIL",
                org.vortikal.repository.resourcetype.PropertyType.Type.STRING) });

        return true;
    }

}

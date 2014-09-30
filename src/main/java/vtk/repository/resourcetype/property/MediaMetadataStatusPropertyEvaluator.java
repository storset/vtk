package vtk.repository.resourcetype.property;

import vtk.repository.Property;
import vtk.repository.PropertyEvaluationContext;
import vtk.repository.PropertyEvaluationContext.Type;
import vtk.repository.resourcetype.PropertyEvaluator;

public class MediaMetadataStatusPropertyEvaluator implements PropertyEvaluator {

    @Override
    public boolean evaluate(Property property, PropertyEvaluationContext ctx) throws PropertyEvaluationException {

        if (property.isValueInitialized() && ctx.getEvaluationType() != Type.ContentChange
                && ctx.getEvaluationType() != Type.Create) {
            return true;
        }
        
        property.setStringValue("GENERATE");

        return true;
    }

}

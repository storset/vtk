package org.vortikal.edit.editor;

import org.vortikal.repository.Property;
import org.vortikal.util.text.TextUtils;

public class TagsEditPreprocessor implements PropertyEditPreprocessor {

    public String preprocess(String valueString, Property prop) {
        return TextUtils.removeDuplicatesIgnoreCaseCapitalized(valueString, ",");
    }

}

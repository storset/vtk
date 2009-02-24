package org.vortikal.edit.editor;

import org.vortikal.repository.Property;

public interface PropertyEditPreprocessor {
    public String preprocess(String valueString, Property prop);
}

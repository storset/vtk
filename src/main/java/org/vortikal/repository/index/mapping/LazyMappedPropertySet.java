/* Copyright (c) 2012, University of Oslo, Norway
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 *  * Neither the name of the University of Oslo nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.vortikal.repository.index.mapping;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Fieldable;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;

/**
 * Lazyily mapped
 * <code>PropertySet</code>. Does minimal work in constructor, and maps
 * on-demand requested props from internal Lucene fields.
 */
class LazyMappedPropertySet implements PropertySet {

    private Path uri;
    private String resourceType;
    private List<Fieldable> propFields;
    private DocumentMapper mapper;

    LazyMappedPropertySet(Document doc, DocumentMapper mapper) throws DocumentMappingException {
        for (Fieldable f : doc.getFields()) {
            if (FieldNames.URI_FIELD_NAME.equals(f.name())) {
                uri = Path.fromString(f.stringValue());
                continue;
            }

            if (FieldNames.RESOURCETYPE_FIELD_NAME.equals(f.name())) {
                resourceType = f.stringValue();
                continue;
            }

            if (FieldNames.isReservedField(f.name())) {
                continue;
            }

            if (propFields == null) {
                propFields = new ArrayList<Fieldable>();
            }

            propFields.add(f);
        }

        if (uri == null) {
            throw new DocumentMappingException("Document missing required field "
                    + FieldNames.URI_FIELD_NAME + ": " + doc);
        }
        if (resourceType == null) {
            throw new DocumentMappingException("Document missing required field "
                    + FieldNames.RESOURCETYPE_FIELD_NAME + ": " + doc);
        }
        this.mapper = mapper;
    }

    @Override
    public Path getURI() {
        return this.uri;
    }

    @Override
    public String getName() {
        return this.uri.getName();
    }

    @Override
    public String getResourceType() {
        return this.resourceType;
    }

    @Override
    public List<Property> getProperties() {
        if (propFields == null) {
            return new ArrayList<Property>(0);
        }

        // Lucene guarantees stored field order to be same as when document was indexed
        final List<Property> props = new ArrayList<Property>(propFields.size());
        for (int i = 0; i < propFields.size(); i++) {
            Fieldable f = propFields.get(i);
            List<Fieldable> values = new ArrayList<Fieldable>();
            values.add(f);
            while (i < propFields.size() - 1 && f.name() == propFields.get(i + 1).name()) { // Interned string comparison OK
                values.add(propFields.get(++i));
            }
            props.add(mapper.getPropertyFromStoredFieldValues(f.name(), values));
        }
        return props;
    }

    @Override
    public List<Property> getProperties(Namespace namespace) {
        if (propFields == null) {
            return new ArrayList<Property>(0);
        }

        // Lucene guarantees stored field order to be same as when document was indexed
        final List<Property> props = new ArrayList<Property>();
        for (int i = 0; i < propFields.size(); i++) {
            final Fieldable f = propFields.get(i);
            List<Fieldable> values = null;
            if (FieldNames.isStoredFieldInNamespace(f.name(), namespace)) {
                values = new ArrayList<Fieldable>();
                values.add(f);
            }
            while (i < propFields.size() - 1 && f.name() == propFields.get(i + 1).name()) { // Interned string comparison OK
                ++i;
                if (values != null) {
                    values.add(propFields.get(i));
                }
            }
            if (values != null) {
                props.add(mapper.getPropertyFromStoredFieldValues(f.name(), values));
            }
        }

        return props;
    }

    @Override
    public Property getProperty(PropertyTypeDefinition def) {
        return getProperty(def.getNamespace(), def.getName());
    }

    @Override
    public Property getProperty(Namespace namespace, String name) {
        if (propFields == null) {
            return null;
        }

        // Lucene guarantees stored field order to be same as when document was indexed
        final String fieldName = FieldNames.getStoredFieldName(namespace, name);
        List<Fieldable> values = null;
        for (Fieldable f : propFields) {
            if (fieldName.equals(f.name())) {
                if (values == null) {
                    values = new ArrayList<Fieldable>();
                }
                values.add(f);
            } else if (values != null) break; // All fields for property collected.
        }

        return values != null ? mapper.getPropertyFromStoredFieldValues(fieldName, values) : null;
    }

    @Override
    public Property getPropertyByPrefix(String prefix, String name) {
        if (propFields == null) {
            return null;
        }

        // Lucene guarantees stored field order to be same as when document was indexed
        final String fieldName = FieldNames.getStoredFieldName(prefix, name);
        List<Fieldable> values = null;
        for (Fieldable f : propFields) {
            if (fieldName.equals(f.name())) {
                if (values == null) {
                    values = new ArrayList<Fieldable>();
                }
                values.add(f);
            } else if (values != null) break; // All fields for property collected.
        }

        return values != null ? mapper.getPropertyFromStoredFieldValues(fieldName, values) : null;
    }

    @Override
    public Iterator<Property> iterator() {
        return getProperties().iterator();
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("PropertySet");
        sb.append(" [").append(uri).append(", resourcetype = ").append(resourceType).append("]");
        return sb.toString();
    }
}

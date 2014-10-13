/* Copyright (c) 2014, University of Oslo, Norway
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
package vtk.repository;

import java.io.InputStreamReader;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.context.ApplicationListener;

import vtk.repository.event.RepositoryEvent;
import vtk.repository.resourcetype.BinaryValue;
import vtk.repository.resourcetype.PropertyTypeDefinition;
import vtk.repository.resourcetype.Value;
import vtk.security.Principal;
import vtk.util.io.StreamUtil;
import vtk.web.service.Service;
import vtk.web.service.URL;


public class AMQPEventDumper extends AbstractRepositoryEventDumper 
    implements ApplicationListener<RepositoryEvent> {
    
    private static Log logger = LogFactory.getLog(AMQPEventDumper.class);

    private AmqpTemplate template;
    private Service urlGenerator;
    private Map<String, PropertySerializer> serializers = null;
    
    public final class SerializationContext {
        private JSONObject json;
        private Resource resource;
        private URL url;
        public SerializationContext(JSONObject json, Resource resource, URL url) 
            { this.json = json; this.resource = resource; this.url = url; }
        public Resource resource() { return resource; }
        public URL url() { return url; }
        public void field(String name, Object value) {
            json.put(name, value);
        }
    }
    
    public interface PropertySerializer {
        /**
         * Serializes a single property
         * @param property the property to serialize
         * @param dest the JSON object containing the properties
         */
        public void serialize(Property property, SerializationContext context) throws Exception;
    }

    public static class SimplifiedLinksSerializer implements PropertySerializer {
        
        @Override
        public void serialize(Property property, SerializationContext context) throws Exception {
            
            PropertyTypeDefinition def = property.getDefinition();
            if (!"links".equals(def.getName())) return;
            JSONArray valueArray = new JSONArray();

            BinaryValue binVal = property.getValue().getBinaryValue();
            String str = StreamUtil.streamToString(binVal.getContentStream().getStream(), "utf-8");
            
            Object data = JSONValue.parse(str);
            
            if (!(data instanceof JSONArray)) {
                return;
            }
            
            JSONArray elements = (JSONArray) data;
            for (Object o: elements) {
                if (!(o instanceof JSONObject)) continue;
                JSONObject record = (JSONObject) o;
                if (!record.containsKey("url")) continue;
                Object urlObj = record.get("url");
                if (urlObj == null) continue;
                String url = urlObj.toString();
                valueArray.add(url);
            }
            if (valueArray.size() > 0)
                context.field(def.getName(), valueArray);
        }
    }
    
    /**
     * Default property serializer: produces <code>name = value</code> pairs in the destination object
     */
    public static PropertySerializer DEFAULT_PROPERTY_SERIALIZER = new PropertySerializer() {

        @Override
        public void serialize(Property property, SerializationContext context) {
            final PropertyTypeDefinition def = property.getDefinition();
            if (def.isMultiple()) {
                JSONArray valueArray = new JSONArray();
                for (Value val: property.getValues()) {
                    Object mapped = mapToBasicValue(val);
                    if (mapped != null)
                        valueArray.add(mapped);
                }
                context.field(def.getName(), valueArray);
            } else {
                Object mapped = mapToBasicValue(property.getValue());
                if (mapped != null)
                    context.field(def.getName(), mapped);
            }
        }
        
        private Object mapToBasicValue(Value value) {
            switch (value.getType()) {
            case BOOLEAN:
                return value.getBooleanValue();
            case DATE:
            case TIMESTAMP:
                return value.getDateValue().getTime();
            case JSON:
                return value.getJSONValue();
            case INT:
                return value.getIntValue();
            case LONG:
                return value.getLongValue();
            case PRINCIPAL:
                return value.getPrincipalValue().getQualifiedName();
            case BINARY:
                return mapBinaryValue(value.getBinaryValue());
            default:
                return value.getNativeStringRepresentation();
            }
        }
        
        private Object mapBinaryValue(BinaryValue value) {
            if ("application/json".equals(value.getContentType())) {
                return JSONValue.parse(new InputStreamReader(value.getContentStream().getStream()));
            }
           return null;
        }
        
    };
    
    public AMQPEventDumper(AmqpTemplate template, Service urlGenerator)  {
        this(template, urlGenerator, null);
    }
    
    public AMQPEventDumper(AmqpTemplate template, Service urlGenerator, Map<String, PropertySerializer> serializers) {
        this.template = template;
        this.urlGenerator = urlGenerator;
        this.serializers = serializers;
    }
    
    @Override
    public void created(Resource resource) {
        template.convertAndSend(updateMsg(resource, "created"));
    }

    @Override
    public void deleted(Resource resource) {
        template.convertAndSend(deletedMsg(resource.getURI()));
    }

    @Override
    public void moved(Resource resource, Resource from) {
        template.convertAndSend(movedMsg(from.getURI(), resource.getURI()));
    }
    
    @Override
    public void modified(Resource resource, Resource originalResource) {
        template.convertAndSend(updateMsg(resource, "props_modified"));
    }

    @Override
    public void modifiedInheritableProperties(Resource resource,
            Resource originalResource) {
        template.convertAndSend(updateMsg(resource, "props_modified"));
    }

    @Override
    public void contentModified(Resource resource, Resource original) {
        template.convertAndSend(updateMsg(resource, "content_modified"));
    }

    @Override
    public void aclModified(Resource resource, Resource originalResource) {
        template.convertAndSend(updateMsg(resource, "acl_modified"));
    }
    
    private static final String VERSION = "0.1";

    private String updateMsg(Resource resource, String type) {
        JSONObject msg = new JSONObject();
        URL url = urlGenerator.constructURL(resource, 
                new vtk.security.PrincipalImpl("root@localhost", Principal.Type.USER));
        msg.put("version", VERSION);
        msg.put("uri", url.toString());
        msg.put("type", type);

        JSONObject properties = new JSONObject();
        SerializationContext context = new SerializationContext(properties, resource, url);
        properties(context);
        
        JSONObject acl = new JSONObject();
        context = new SerializationContext(acl, resource, url);
        acl(context);
        
        JSONObject resourceObject = new JSONObject();
        resourceObject.put("properties", properties);
        resourceObject.put("acl", acl);
        
        msg.put("data", resourceObject);
        return msg.toString();
    }
    
    private String deletedMsg(Path uri) {
        JSONObject msg = new JSONObject();
        URL url = urlGenerator.constructURL(uri);
        msg.put("version", VERSION);
        msg.put("type", "deleted");
        msg.put("uri", url.toString());
        return msg.toString();
    }

    private String movedMsg(Path from, Path to) {
        JSONObject msg = new JSONObject();
        msg.put("version", VERSION);
        msg.put("type", "moved");
        msg.put("from", urlGenerator.constructURL(from).toString());
        msg.put("to", urlGenerator.constructURL(to).toString());
        return msg.toString();
    }

    
    private void properties(SerializationContext context) {
        
        for (Property prop: context.resource()) {
            PropertyTypeDefinition def = prop.getDefinition();
            PropertySerializer serializer = serializers == null ? 
                    DEFAULT_PROPERTY_SERIALIZER : serializers.get(def.getName());
            if (serializer == null) continue;
            try {
                serializer.serialize(prop, context);
            } catch (Exception e) {
                logger.error("Failed to serialize property " + 
                        prop + " of resource " + context.resource(), e);
            }
        }
    }

    private void acl(SerializationContext context) {
        Acl acl = context.resource().getAcl();
        Set<Privilege> actions = acl.getActions();

        for (Privilege action: actions) {
            JSONArray array = new JSONArray();
            for (Principal p: acl.getPrincipalSet(action)) {
                array.add(p.getQualifiedName());
            }
            context.field(action.getName(), array);
        }
    }
    
}

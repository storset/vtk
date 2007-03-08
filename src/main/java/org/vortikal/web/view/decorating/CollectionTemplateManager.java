/* Copyright (c) 2007, University of Oslo, Norway
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
package org.vortikal.web.view.decorating;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;

import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.ResourceTypeDefinition;


/**
 * A template manager that loads templates from a specified collection
 * in a {@link Repository content repository}.
 *
 * <p>Configurable JavaBean properties:
 * <ul>
 *   <li><code>repository</code> - the {@link Repository content repository}
 *   <li><code>collectionName</code> - the complete path to the
 *   templates collection, e.g. <code>/foo/bar/templates</code>.
 *   <li><code>templateParser</code> - a {@link Templateparser template parser}
 *   <li><code>templateResourceType</code> - the {@link
 *   ResourceTypeDefinition resource type} identifying templates (all
 *   candidate templates must be of this resource type).
 * </ul>
 *
 */
public class CollectionTemplateManager implements TemplateManager, InitializingBean {

    private static Log logger = LogFactory.getLog(CollectionTemplateManager.class);

    private Repository repository;
    private String collectionName;
    private TemplateParser templateParser;
    private ResourceTypeDefinition templateResourceType;
    private Map templatesMap;
    

    public void setRepository(Repository repository) {
        this.repository = repository;
    }


    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }


    public void setTemplateParser(TemplateParser templateParser) {
        this.templateParser = templateParser;
    }
    

    public void setTemplateResourceType(ResourceTypeDefinition templateResourceType) {
        this.templateResourceType = templateResourceType;
    }
    

    public void afterPropertiesSet() {
        if (this.repository == null) {
            throw new BeanInitializationException(
                "JavaBean property 'repository' not specified");
        }
        if (this.collectionName == null) {
            throw new BeanInitializationException(
                "JavaBean property 'collectionName' not specified");
        }
        if (this.templateParser == null) {
            throw new BeanInitializationException(
                "JavaBean property 'templateParser' not specified");
        }
        if (this.templateResourceType == null) {
            throw new BeanInitializationException(
                "JavaBean property 'templateResourceType' not specified");
        }
    }
    

    public Template getTemplate(String name) throws Exception {
        if (this.templatesMap == null) {
            load();
        }
        if (this.templatesMap == null) {
            return null;
        }

        Template template = (Template) this.templatesMap.get(name);
        if (logger.isDebugEnabled()) {
            logger.debug("Resolved name '" + name + "' to template '"
                         + template + "'");
        }
        return template;
    }


    public synchronized void load() throws Exception {

        Map map = new HashMap();
        Resource[] resources =
            this.repository.listChildren(null, this.collectionName, true);

        int numTemplates = 0;

        for (int i = 0; i < resources.length; i++) {

            if (resources[i].isOfType(this.templateResourceType)) {
                TemplateSource templateSource =
                    new RepositoryTemplateSource(resources[i].getURI(), this.repository, null);

                try {
                    Template template = new StandardDecoratorTemplate(this.templateParser, templateSource);
                    if (logger.isDebugEnabled()) {
                        logger.debug("Loaded template '" + resources[i].getName() + "'");
                    }
                    map.put(resources[i].getName(), template);
                    numTemplates++;
                } catch (Throwable t) {
                    logger.info("Unable to compile template from resource " + resources[i], t);
                }
            }
        }
        if (logger.isInfoEnabled()) {
            logger.info("Loaded " + numTemplates + " template(s) from collection '"
                        + this.collectionName + "'");
        }

        this.templatesMap = map;
    }
    

}



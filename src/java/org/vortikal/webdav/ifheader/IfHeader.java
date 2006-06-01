/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.vortikal.webdav.ifheader;

import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;

import org.vortikal.repository.Resource;

public interface IfHeader {

    public boolean matches(Resource resource, boolean shouldMatchOnNoIfHeader);
    
    //public boolean matchesEtags(Resource resource, boolean shouldMatchOnNoIfHeader);
    
    public Iterator getAllTokens();

    public Iterator getAllNotTokens();

    public boolean hasTokens();

}

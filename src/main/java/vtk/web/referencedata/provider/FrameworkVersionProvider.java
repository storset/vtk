/* Copyright (c) 2004, University of Oslo, Norway
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
package vtk.web.referencedata.provider;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import vtk.util.Version;
import vtk.web.referencedata.ReferenceDataProvider;

/**
 * Reference data provider for information about the current version of
 * the framework.  The information is made available in the model as
 * a submodel of the name <code>version</code>.
 * 
 * Model data provided:
 * <ul>
 *   <li><code>version</code> - the current version
 *   <li><code>buildDate</code> - the build date of the framework
 *   <li><code>buildHost</code> - the host of the build
 *   <li><code>buildVendor</code> - 
 *   <li><code>frameworkTitle</code> - 
 * </ul>
 */
public class FrameworkVersionProvider implements ReferenceDataProvider {

    @Override
    public void referenceData(Map<String, Object> model, HttpServletRequest request)
        throws Exception {
        Map<String, Object> versionModel = new HashMap<String, Object>();
        versionModel.put("version", Version.getVersion());
        versionModel.put("buildDate", Version.getBuildDate());
        versionModel.put("buildHost", Version.getBuildHost());
        versionModel.put("buildVendor", Version.getBuildVendor());
        versionModel.put("frameworkTitle", Version.getFrameworkTitle());
        model.put("version", versionModel);
    }
}

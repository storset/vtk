/* Copyright (c) 2012 University of Oslo, Norway
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
package org.vortikal.web.actions.copymove;

import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.vortikal.repository.Path;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.ResourceImpl;
import org.vortikal.repository.Repository.Depth;
import org.vortikal.web.actions.convert.CopyAction;

public class CopyHelper {

    private static final Pattern COPY_SUFFIX_PATTERN = Pattern.compile("\\(\\d+\\)$");
    private CopyAction copyAction;
    private CopyThenStoreAction copyThenStoreAction;

    public Path copyResource (Path uri, Path destUri, Repository repository, String token, Resource src, InputStream is) throws Exception {
        int number = 1;
        while (repository.exists(token, destUri)) {
            destUri = appendCopySuffix(destUri, number);
            number++;
        }
        if (this.copyThenStoreAction != null && src != null && is != null) {
            this.copyThenStoreAction.process(destUri, src, is);
        } else if (this.copyAction != null) {
            this.copyAction.process(uri, destUri, null);
        } else {
            repository.copy(token, uri, destUri, Depth.INF, false, false);
        }
        return destUri;
    }

    protected Path appendCopySuffix(Path newUri, int number) {
        String extension = "";
        String dot = "";
        String name = newUri.getName();

        if (name.endsWith(".")) {
            name = name.substring(0, name.lastIndexOf("."));

        } else if (name.contains(".")) {
            extension = name.substring(name.lastIndexOf(".") + 1, name.length());
            dot = ".";
            name = name.substring(0, name.lastIndexOf("."));
        }

        Matcher matcher = COPY_SUFFIX_PATTERN.matcher(name);
        if (matcher.find()) {
            String count = matcher.group();
            count = count.substring(1, count.length() - 1);
            try {
                number = Integer.parseInt(count) + 1;
                name = COPY_SUFFIX_PATTERN.split(name)[0];
            } catch (Exception e) {
            }
        }

        name = name + "(" + number + ")" + dot + extension;
        return newUri.getParent().extend(name);
    }
    
    public void setCopyAction(CopyAction copyAction) {
        this.copyAction = copyAction;
    }
    
    public void setCopyThenStoreAction(CopyThenStoreAction copyThenStoreAction) {
        this.copyThenStoreAction = copyThenStoreAction;
    }

}

/* Copyright (c) 2005, University of Oslo, Norway
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
package org.vortikal.web.controller.index;

import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.vortikal.repositoryimpl.index.Index;
import org.vortikal.repositoryimpl.index.management.IndexStatus;
import org.vortikal.repositoryimpl.index.management.ManagementException;
import org.vortikal.repositoryimpl.index.management.Reindexer;
import org.vortikal.repositoryimpl.index.management.RuntimeManager;
import org.vortikal.repositoryimpl.index.observation.ResourceChangeObserver;
import org.vortikal.web.RequestContext;

/**
 * Magic parameter based web-interface for controlling the index runtime manager.
 * 
 * Needed for production environment to control/run re-indexing, amongst other
 * things. Sports a fantastic UI, by returning status reports in text/plain. 
 * 
 * This handler should only be reachable from a service whose ancestor(s) or itself
 * contains a "principal=root@localhost"-assertion.
 * 
 * Complaints to author about ugly hacks will be sent to /dev/null.
 * This controller is a temporary solution, meant for testing purposes
 * only, it's simple and it works. Creating a different 
 * interface for the RuntimeManager should be easy, later on.
 *
 * FIXME: Use some kind of console support instead ?
 *  
 * @author oyviste
 *
 */
public final class IndexManagementController implements Controller {
    
    private static final String ACTION_REINDEX_CURRENT = "reindex-current-tree";
    private static final String ACTION_REINDEX_ALL = "reindex-all";
    private static final String ACTION_STOP = "stop";
    private static final String ACTION_STATUS = "status";
    private static final String ACTION_DISABLE_OBSERVER = "disable-observer";
    private static final String ACTION_ENABLE_OBSERVER = "enable-observer";
    private static final String ACTION_OPTIMIZE = "optimize";
    private static final String ACTION_HELP = "help";
    
    private RuntimeManager runtimeManager;
    //private String viewName;
    
    public ModelAndView handleRequest(HttpServletRequest request, 
                                      HttpServletResponse response)
            throws Exception {
        
        RequestContext requestContext = RequestContext.getRequestContext();
        String uri = requestContext.getResourceURI();

        response.setContentType("text/plain");
        PrintWriter writer = response.getWriter();
        
        writer.println("INDEX RUNTIME MANAGEMENT SERVICE");

        String action = request.getParameter("action");
        String id = request.getParameter("id");
        try {
            if (action == null) {
                writer.println("* MISSING 'action' HTTP parameter. Supported operations: ");
                printUsage(writer, uri);
                printStatus(writer);
                return null;
            } 
            
            if (ACTION_STATUS.equals(action)) {
                printStatus(writer);
                return null;
            } 
            
            if (ACTION_HELP.equals(action)) {
                printUsage(writer, uri);
                return null;
            } 
            
            if (id == null) {
                writer.println("* MISSING 'id' HTTP parameter.");
                printUsage(writer, uri);
                printStatus(writer);
                return null;
            } 
            
            if (ACTION_REINDEX_CURRENT.equals(action) || ACTION_REINDEX_ALL.equals(action)) {
                Index index = runtimeManager.getIndex(id);
                Reindexer reindexer = runtimeManager.getReindexerForIndex(index);
                if (reindexer == null) {
                    writer.println("* No re-indexer configured for index '" + id + "'");
                    printStatus(writer);
                    return null;
                } 
                
                if (ACTION_REINDEX_CURRENT.equals(action)) {
                    writer.println("* Starting re-indexer on subtree '" + uri + 
                                   "' for index '" + id + "'");
                    reindexer.start(uri);
                } else {
                    writer.println("* Starting re-indexing of entire host for index '" +
                                   id + "', index will be re-created.");
                    reindexer.start();
                }
            } else if (ACTION_STOP.equals(action)) {
                Index index = runtimeManager.getIndex(id);
                Reindexer reindexer = runtimeManager.getReindexerForIndex(index);
                if (reindexer == null) {
                    writer.println("* No re-indexer configured for index '" + id + "'");
                    printStatus(writer);
                    return null;
                }
                writer.println("* Signalling re-indexer to stop ..");
                writer.flush();
                reindexer.stop();
            } else if (ACTION_DISABLE_OBSERVER.equals(action)) {
                ResourceChangeObserver observer = runtimeManager.getObserver(id);
                writer.println("* Disabling observer '" + id + "' ..");
                writer.flush();
                observer.disable();
            } else if (ACTION_ENABLE_OBSERVER.equals(action)) {
                ResourceChangeObserver observer = runtimeManager.getObserver(id);
                writer.println("* Enabling observer '" + id + "' ..");
                writer.flush();
                observer.enable();
            } else if (ACTION_OPTIMIZE.equals(action)) {
                writer.println("* Optimizing index '" + id + "' ..");
                writer.flush();
                runtimeManager.optimizeIndex(runtimeManager.getIndex(id));
            } else {
                writer.println("* Unknown action '" + action + "'");
                printUsage(writer, uri);
            }
            
            printStatus(writer);
        } catch (ManagementException me) {
            writer.println("* Index management exception: " + me.getMessage());
        } finally {
            writer.close();
        }
        return null;
    }
    
    private void printStatus(PrintWriter writer) {
        writer.println();
        writer.println("* STATUS");
        List indexes = runtimeManager.getIndexes();
        List observers = runtimeManager.getObservers();
        printIndexList(writer, indexes);
        writer.println();
        printObserverList(writer, observers);
    }
    
    private void printIndexList(PrintWriter writer, List list) { 
        writer.println("* REGISTERED INDEXES:");
        for (Iterator iter = list.iterator(); iter.hasNext();) {
            try {
                Index index = (Index)iter.next();
                IndexStatus status = runtimeManager.getStatusForIndex(index);
                writer.println("  + ID: '" + index.getIndexId() + "'");
                writer.println("    - Index system path: '" + status.getSystemPath() + "'");
                writer.println("    - Locked: " + status.isLocked());
                writer.println("    - Has deletions: " + status.hasDeletions());
                writer.println("    - Number of docs: " + status.getNumberOfDocuments());
                writer.println("    - Total size in bytes: " + status.getPhysicalSize());
                
                if (runtimeManager.getReindexerForIndex(index) != null) {
                    Reindexer r = runtimeManager.getReindexerForIndex(index);
                    writer.println("    + Configured re-indexer:");
                    writer.println("      - Running: " + r.isRunning());
                    writer.println("      - Current working tree: " + r.getCurrentWorkingTree());
                    writer.println("      - Asynchronous mode: " + r.isAsynchronous());
                    if (r.getFilter() != null) {
                        writer.println("      - Filter: " + r.getFilter().toString());                    
                    } else {
                        writer.println("      - Filter: none.");
                    }
    
                    writer.println("      - Skip filtered subtress: " + r.isSkipFilteredSubtrees());
                    writer.println("      - Other info: " + r.toString());
                }
            // Don't let management exceptions for one index prevent status
            // from being printed out for the others.
            } catch (ManagementException me) {
                writer.print("Problems getting status for index: " + me.getMessage());
                writer.println();
            }
            writer.println();
        }
    }
    
    private void printObserverList(PrintWriter writer, List list) {
        writer.println("* REGISTERED RESOURCE CHANGE OBSERVERS:");
        for (Iterator iter = list.iterator(); iter.hasNext();) {
            ResourceChangeObserver observer = (ResourceChangeObserver)iter.next();
            writer.println("  + ID: '" + observer.getObserverId() + "'");
            writer.println("    - Enabled: " + observer.isEnabled());
            writer.println("    - Other info: " + observer.toString());
            writer.println();
        }
    }    

    private void printUsage(PrintWriter writer, String uri) {
        writer.println();
        writer.println("* USAGE:");
        writer.println("  action=reindex-current-tree  Re-index current '" + uri + "' subtree.");
        writer.println("  action=reindex-all           Reindex all resources on host.");
        writer.println("  action=stop                  Stop a running re-indexing operation (only works in asynchronous mode.).");
        writer.println();
        writer.println("  action=optimize              Optimize an index.");
        writer.println();
        writer.println("  action=disable-observer      Disable a resource change observer.");
        writer.println("  action=enable-observer       Enable a resource change observer.");
        writer.println();
        writer.println("  id=[observerId|indexId]      Id of observer or index to perform action on.");
        writer.println();
        writer.println("  action=status                Print status report.");
    }
    
    public void setRuntimeManager(RuntimeManager runtimeManager) {
        this.runtimeManager = runtimeManager;
    }

//    public void setViewName(String viewName) {
//        this.viewName = viewName;
//    }

}

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
package org.vortikal.web.controller;



/**
 * Utility base class for form command objects that have 'Save' and
 * 'Cancel' actions.
 */
public class UpdateCancelCommand {

    private String saveAction = null;
    private String cancelAction = null;
    private String submitURL;
    private boolean done = false;


    /**
     * Creates a new <code>AbstractSaveCancelCommand</code> instance.
     *
     * @param submitURL the URL of the form submission.
     */
    public UpdateCancelCommand(String submitURL) {
        this.submitURL = submitURL;
    }
    

    /**
     * Gets the value of saveAction
     *
     * @return the value of saveAction
     */
    public String getSaveAction() {
        return this.saveAction;
    }

    /**
     * Sets the value of saveAction
     *
     * @param saveAction Value to assign to this.saveAction
     */
    public void setSaveAction(String saveAction) {
        this.saveAction = saveAction;
    }

    /**
     * Gets the value of cancelAction
     *
     * @return the value of cancelAction
     */
    public String getCancelAction() {
        return this.cancelAction;
    }

    /**
     * Sets the value of cancelAction
     *
     * @param cancelAction Value to assign to this.cancelAction
     */
    public void setCancelAction(String cancelAction) {
        this.cancelAction = cancelAction;
    }
    

    /**
     * Gets the value of submitURL
     *
     * @return the value of submitURL
     */
    public String getSubmitURL() {
        return this.submitURL;
    }


    /**
     * Sets the value of submitURL
     *
     * @param submitURL Value to assign to this.submitURL
     */
    public void setSubmitURL(String submitURL)  {
        this.submitURL = submitURL;
    }

    
    /**
     * @return Returns the done.
     */
    public boolean isDone() {
        return this.done;
    }
    

    /**
     * @param done The done to set.
     */
    public void setDone(boolean done) {
        this.done = done;
    }
}

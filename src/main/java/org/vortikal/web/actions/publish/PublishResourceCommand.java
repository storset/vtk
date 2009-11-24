package org.vortikal.web.actions.publish;

import org.vortikal.web.actions.UpdateCancelCommand;

public class PublishResourceCommand extends UpdateCancelCommand {
    
    private String publishResourceAction = null;
    private String publishResourceCancelAction = null;

    public PublishResourceCommand(String submitURL) {
        super(submitURL);
    }

    public String getPublishResourceAction() {
        return publishResourceAction;
    }

    public void setPublishResourceAction(String publishResourceAction) {
        this.publishResourceAction = publishResourceAction;
    }

    public String getPublishResourceCancelAction() {
        return publishResourceCancelAction;
    }

    public void setPublishResourceCancelAction(String publishResourceCancelAction) {
        this.publishResourceCancelAction = publishResourceCancelAction;
    }

}

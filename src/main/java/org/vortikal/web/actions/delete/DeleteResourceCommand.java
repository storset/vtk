package org.vortikal.web.actions.delete;

import org.vortikal.web.actions.UpdateCancelCommand;

public class DeleteResourceCommand extends UpdateCancelCommand {
	private String deleteResourceAction = null;
	private String deleteResourceCancelAction = null;

	public DeleteResourceCommand(String submitURL) {
		super(submitURL);
	}

	public String getDeleteResourceAction() {
		return deleteResourceAction;
	}

	public void setDeleteResourceAction(String deleteResourceAction) {
		this.deleteResourceAction = deleteResourceAction;
	}

	public String getDeleteResourceCancelAction() {
		return deleteResourceCancelAction;
	}

	public void setDeleteResourceCancelAction(String deleteResourceCancelAction) {
		this.deleteResourceCancelAction = deleteResourceCancelAction;
	}
	
}

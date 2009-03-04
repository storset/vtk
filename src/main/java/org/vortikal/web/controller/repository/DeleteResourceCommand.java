package org.vortikal.web.controller.repository;

import org.vortikal.web.controller.UpdateCancelCommand;

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

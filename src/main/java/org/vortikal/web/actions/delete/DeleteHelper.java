package org.vortikal.web.actions.delete;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.vortikal.repository.AuthorizationException;
import org.vortikal.repository.Path;
import org.vortikal.repository.Repository;
import org.vortikal.repository.ResourceLockedException;
import org.vortikal.web.Message;
import org.vortikal.web.RequestContext;

public class DeleteHelper {

	protected final String msgKey = "manage.delete.error.";
	
	private static Log logger = LogFactory.getLog(DeleteHelper.class);

	public void deleteResource(Repository repository, String token, Path uri, boolean recoverable, Map<String, List<Path>> failures) {
		try {
			if (repository.exists(token, uri)) {
				repository.delete(token, uri, recoverable);
			} else {
				this.addToFailures(failures, uri, this.msgKey, "nonExisting");
			}
		} catch (IllegalArgumentException iae) { // Not a path, ignore it	
		} catch (AuthorizationException ae) {
			this.addToFailures(failures, uri, this.msgKey, "unAuthorized");
		} catch (ResourceLockedException rle) {
			this.addToFailures(failures, uri, this.msgKey, "locked");
		} catch (Exception ex) {
			StringBuilder msg = new StringBuilder("Could not perform ");
			msg.append("delete of ").append(uri);
			msg.append(": ").append(ex.getMessage());
			logger.warn(msg);
			this.addToFailures(failures, uri, this.msgKey, "generic");
		}	
	}
	
	public void addToFailures(Map<String, List<Path>> failures, Path fileUri, String msgKey, String failureType) {
		String key = msgKey.concat(failureType);
		List<Path> failedPaths = failures.get(key);
		if (failedPaths == null) {
			failedPaths = new ArrayList<Path>();
			failures.put(key, failedPaths);
		}
		failedPaths.add(fileUri);
	}

	public void addFailureMessages(Map<String, List<Path>> failures, RequestContext requestContext) {
		for (Entry<String, List<Path>> entry : failures.entrySet()) {
			String key = entry.getKey();
			List<Path> failedResources = entry.getValue();
			Message msg = new Message(key);
			for (Path p : failedResources) {
				msg.addMessage(p.getName());
			}
			requestContext.addErrorMessage(msg);
		}
	}
}

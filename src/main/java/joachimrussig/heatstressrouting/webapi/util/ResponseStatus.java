package joachimrussig.heatstressrouting.webapi.util;

import javax.ws.rs.core.Response.Status;

public enum ResponseStatus {

	OK, NO_REULTS, BAD_REQUEST, INTERNAL_ERROR;

	public int getHttpStatusCode() {
		return toStatus().getStatusCode();
	}
	
	public Status toStatus() {
		switch (this) {
		case OK:
			return Status.OK;
		case NO_REULTS:
			return Status.OK;
		case BAD_REQUEST:
			return Status.BAD_REQUEST;
		default:
			return Status.INTERNAL_SERVER_ERROR;
		}
	}
}

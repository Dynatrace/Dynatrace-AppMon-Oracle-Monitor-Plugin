package com.dynatrace.diagnostics.plugins.exception;

public class ReportCreationException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6869543200215509789L;
	
	public ReportCreationException() {}
	
	public ReportCreationException(String msg) {
		super(msg);
	}
	
	public ReportCreationException(Throwable cause) {
		super(cause);
	}
	
	public ReportCreationException(String msg, Throwable cause) {
		super(msg, cause);
	}

}

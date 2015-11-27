package com.dynatrace.diagnostics.plugins.domain;

public enum LOCK_MODE {
	NONE("None"), 
	NULL("Null"), 
	ROW_S_SS("Row-S (SS)"), 
	ROW_X_SX("Row-X (SX)"), 
	SHARE("Share"), 
	S_ROW_X_SSX("S/Row-X (SSX)"), 
	EXCLUSIVE("Exclusive");

	private String lockMode;

	LOCK_MODE(String lockMode) {
		this.lockMode = lockMode;
	}

	public String lockMode() {
		return lockMode;
	}

	// Optionally and/or additionally, toString.
	@Override
	public String toString() {
		return lockMode;
	}

}

package com.dynatrace.diagnostics.plugins.domain;

public class LocksMetrics {
	private String key;
	private String sidSerial;
	private String oraUser;
	private String osUserName;
	private String owner;
	private String objectName;
	private String objectType;
	private String lockMode;
	private Double lockModeMeasure;
	private String status;
	private Double statusMeasure;
	private String lastDdl;
	private Double lastDdlMeasure;
	
	public String getKey() {
		return key;
	}
	public void setKey(String key) {
		this.key = key;
	}
	public String getSidSerial() {
		return sidSerial;
	}
	public void setSidSerial(String sidSerial) {
		this.sidSerial = sidSerial;
	}
	public String getOraUser() {
		return oraUser;
	}
	public void setOraUser(String oraUser) {
		this.oraUser = oraUser;
	}
	public String getOsUserName() {
		return osUserName;
	}
	public void setOsUserName(String osUserName) {
		this.osUserName = osUserName;
	}
	public String getOwner() {
		return owner;
	}
	public void setOwner(String owner) {
		this.owner = owner;
	}
	public String getObjectName() {
		return objectName;
	}
	public void setObjectName(String objectName) {
		this.objectName = objectName;
	}
	public String getObjectType() {
		return objectType;
	}
	public void setObjectType(String objectType) {
		this.objectType = objectType;
	}
	public String getLockMode() {
		return lockMode;
	}
	public void setLockMode(String lockMode) {
		this.lockMode = lockMode;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public String getLastDdl() {
		return lastDdl;
	}
	public void setLastDdl(String lastDdl) {
		this.lastDdl = lastDdl;
	}
	public Double getLockModeMeasure() {
		return lockModeMeasure;
	}
	public void setLockModeMeasure(Double lockModeMeasure) {
		this.lockModeMeasure = lockModeMeasure;
	}
	public Double getStatusMeasure() {
		return statusMeasure;
	}
	public void setStatusMeasure(Double statusMeasure) {
		this.statusMeasure = statusMeasure;
	}
	public Double getLastDdlMeasure() {
		return lastDdlMeasure;
	}
	public void setLastDdlMeasure(Double lastDdlMeasure) {
		this.lastDdlMeasure = lastDdlMeasure;
	}
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("LocksMetrics [key=").append(key).append(", sidSerial=").append(sidSerial).append(", oraUser=")
				.append(oraUser).append(", osUserName=").append(osUserName).append(", owner=").append(owner)
				.append(", objectName=").append(objectName).append(", objectType=").append(objectType)
				.append(", lockMode=").append(lockMode).append(", lockModeMeasure=").append(lockModeMeasure)
				.append(", status=").append(status).append(", statusMeasure=").append(statusMeasure)
				.append(", lastDdl=").append(lastDdl).append(", lastDdlMeasure=").append(lastDdlMeasure).append("]");
		return builder.toString();
	}
}

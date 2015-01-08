package com.dynatrace.diagnostics.plugins.domain;

import java.util.Date;

public class SqlMetrics {
	private String sid;
	private String sqlFulltext;
	private String key;
	private Double executions;
	private Double elapsedTime;
	private Double averageElapsedTime;
	private Double cpuTime;
	private Double averageCpuTime;
	private Double diskReads;
	private Double directWrites;
	private Double bufferGets;
	private Double rowsProcessed;
	private Double parseCalls;
	private String firstLoadTime;
	private Date firstLoadTimeDate;
	private String lastLoadTime;
	private Date lastLoadTimeDate;
	private Double childNumber;
	private Long planHashValue;
	private Long explainPlanId;
	private String explainPlan;
	private Long srcDbNameId;
	
	public String getSid() {
		return sid;
	}
	public void setSid(String sid) {
		this.sid = sid;
	}
	public String getSqlFulltext() {
		return sqlFulltext;
	}
	public void setSqlFulltext(String sql) {
		this.sqlFulltext = sql;
	}
	public String getKey() {
		return key;
	}
	public void setKey(String key) {
		this.key = key;
	}
	public Double getExecutions() {
		return executions;
	}
	public void setExecutions(Double executions) {
		this.executions = executions;
	}
	public Double getElapsedTime() {
		return elapsedTime;
	}
	public void setElapsedTime(Double elapsedTime) {
		this.elapsedTime = elapsedTime;
	}
	public Double getAverageElapsedTime() {
		return averageElapsedTime;
	}
	public void setAverageElapsedTime(Double averageElapsedTime) {
		this.averageElapsedTime = averageElapsedTime;
	}
	public Double getCpuTime() {
		return cpuTime;
	}
	public void setCpuTime(Double cpuTime) {
		this.cpuTime = cpuTime;
	}
	public Double getAverageCpuTime() {
		return averageCpuTime;
	}
	public void setAverageCpuTime(Double averageCpuTime) {
		this.averageCpuTime = averageCpuTime;
	}
	public Double getDiskReads() {
		return diskReads;
	}
	public void setDiskReads(Double diskReads) {
		this.diskReads = diskReads;
	}
	public Double getDirectWrites() {
		return directWrites;
	}
	public void setDirectWrites(Double directWrites) {
		this.directWrites = directWrites;
	}
	public Double getBufferGets() {
		return bufferGets;
	}
	public void setBufferGets(Double bufferGets) {
		this.bufferGets = bufferGets;
	}
	public Double getRowsProcessed() {
		return rowsProcessed;
	}
	public void setRowsProcessed(Double rowsProcessed) {
		this.rowsProcessed = rowsProcessed;
	}
	public Double getParseCalls() {
		return parseCalls;
	}
	public void setParseCalls(Double parseCalls) {
		this.parseCalls = parseCalls;
	}
	public String getFirstLoadTime() {
		return firstLoadTime;
	}
	public void setFirstLoadTime(String firstLoadTime) {
		this.firstLoadTime = firstLoadTime;
	}
	public Date getFirstLoadTimeDate() {
		return firstLoadTimeDate;
	}
	public void setFirstLoadTimeDate(Date firstLoadTimeDate) {
		this.firstLoadTimeDate = firstLoadTimeDate;
	}
	public String getLastLoadTime() {
		return lastLoadTime;
	}
	public void setLastLoadTime(String lastLoadTime) {
		this.lastLoadTime = lastLoadTime;
	}
	public Date getLastLoadTimeDate() {
		return lastLoadTimeDate;
	}
	public void setLastLoadTimeDate(Date lastLoadTimeDate) {
		this.lastLoadTimeDate = lastLoadTimeDate;
	}
	public Double getChildNumber() {
		return childNumber;
	}
	public void setChildNumber(Double childNumber) {
		this.childNumber = childNumber;
	}
	public Long getPlanHashValue() {
		return planHashValue;
	}
	public void setPlanHashValue(Long planHashValue) {
		this.planHashValue = planHashValue;
	}
	public String getExplainPlan() {
		return explainPlan;
	}
	public Long getExplainPlanId() {
		return explainPlanId;
	}
	public void setExplainPlanId(Long explainPlanId) {
		this.explainPlanId = explainPlanId;
	}
	public void setExplainPlan(String explainPlan) {
		this.explainPlan = explainPlan;
	}
	public Long getSrcDbNameId() {
		return srcDbNameId;
	}
	public void setSrcDbNameId(Long srcDbNameId) {
		this.srcDbNameId = srcDbNameId;
	}
	

}

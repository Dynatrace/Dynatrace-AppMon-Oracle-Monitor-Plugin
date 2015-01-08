package com.dynatrace.diagnostics.plugins.domain;

public class TablespacesMetrics {
	private String key;
	private String name;
	private Double total;
	private Double used;
	private Double free;
	private Double percentUsed;
	private Double percentFree;
	
	public String getKey() {
		return key;
	}
	public void setKey(String key) {
		this.key = key;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Double getTotal() {
		return total;
	}
	public void setTotal(Double total) {
		this.total = total;
	}
	public Double getUsed() {
		return used;
	}
	public void setUsed(Double used) {
		this.used = used;
	}
	public Double getFree() {
		return free;
	}
	public void setFree(Double free) {
		this.free = free;
	}
	public Double getPercentUsed() {
		return percentUsed;
	}
	public void setPercentUsed(Double percentUsed) {
		this.percentUsed = percentUsed;
	}
	public Double getPercentFree() {
		return percentFree;
	}
	public void setPercentFree(Double percentFree) {
		this.percentFree = percentFree;
	}
	
}

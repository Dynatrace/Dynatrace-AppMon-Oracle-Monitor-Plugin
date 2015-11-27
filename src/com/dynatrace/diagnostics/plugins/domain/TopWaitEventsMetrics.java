package com.dynatrace.diagnostics.plugins.domain;

public class TopWaitEventsMetrics {
	private String key;
	private String event;
	private Double totalWaits;
	private Double totalTimeouts;
	private Double timeWaited; // in seconds
	private Double averageWait; // in seconds
	public String getKey() {
		return key;
	}
	public void setKey(String key) {
		this.key = key;
	}
	public String getEvent() {
		return event;
	}
	public void setEvent(String event) {
		this.event = event;
	}
	public Double getTotalWaits() {
		return totalWaits;
	}
	public void setTotalWaits(Double totalWaits) {
		this.totalWaits = totalWaits;
	}
	public Double getTotalTimeouts() {
		return totalTimeouts;
	}
	public void setTotalTimeouts(Double totalTimeouts) {
		this.totalTimeouts = totalTimeouts;
	}
	public Double getTimeWaited() {
		return timeWaited;
	}
	public void setTimeWaited(Double timeWaited) {
		this.timeWaited = timeWaited;
	}
	public Double getAverageWait() {
		return averageWait;
	}
	public void setAverageWait(Double averageWait) {
		this.averageWait = averageWait;
	}
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("TopWaitEventsMetrics [key=").append(key).append(", event=").append(event)
				.append(", totalWaits=").append(totalWaits).append(", totalTimeouts=").append(totalTimeouts)
				.append(", timeWaited=").append(timeWaited).append(", averageWait=").append(averageWait).append("]");
		return builder.toString();
	}
	
}

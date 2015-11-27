package com.dynatrace.diagnostics.plugins.domain;

public class CacheHitRatioStats {
	private String namespace;
	private double getHitRatio;
	private double pinHitRatio;
	public String getNamespace() {
		return namespace;
	}
	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}
	public double getGetHitRatio() {
		return getHitRatio;
	}
	public void setGetHitRatio(double getHitRatio) {
		this.getHitRatio = getHitRatio;
	}
	public double getPinHitRatio() {
		return pinHitRatio;
	}
	public void setPinHitRatio(double pinHitRatio) {
		this.pinHitRatio = pinHitRatio;
	}
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("CacheHitRatioStats [namespace=").append(namespace)
				.append(", getHitRatio=").append(getHitRatio)
				.append(", pinHitRatio=").append(pinHitRatio).append("]");
		return builder.toString();
	}
}

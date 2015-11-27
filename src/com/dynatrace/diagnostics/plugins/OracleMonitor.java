package com.dynatrace.diagnostics.plugins;

import java.util.Collection;
import java.util.logging.Logger;

import com.dynatrace.diagnostics.pdk.Monitor;
import com.dynatrace.diagnostics.pdk.MonitorEnvironment;
import com.dynatrace.diagnostics.pdk.MonitorMeasure;
import com.dynatrace.diagnostics.pdk.Status;
import com.dynatrace.diagnostics.pdk.Status.StatusCode;
import com.dynatrace.diagnostics.plugins.utils.HelperUtils;

public class OracleMonitor extends OraclePlugin implements Monitor {
    private static final Logger log = Logger.getLogger(OracleMonitor.class.getName());
	double success = 1;
	double instanceUp = 0;
	double historyInstanceUp = 0;
	
	@Override
	public Status setup(MonitorEnvironment env) throws Exception {
		return super.setup(env);
	}

	@Override
	public Status execute(MonitorEnvironment env) throws Exception {
		log.finer("Entering OracleMonitor.execute method");
		Collection<MonitorMeasure> measures;
		Status result = null;

		try {
			success = 1;
			instanceUp = 0;
			try {
				instanceUp = getInstanceUp(env, false);
				if (instanceUp == 0) {
					// sleep 2 seconds before resetting connection
					try {Thread.sleep(2);} catch (Exception e1){}
					resetConnection(false); // reset connection for a monitored database
					instanceUp = getInstanceUp(env, false);
				}
			} catch(Exception e) {
				try {
					// sleep 2 seconds before resetting connection
					try {Thread.sleep(2);} catch (Exception e1){}
					resetConnection(false); // reset connection for a monitored database
					instanceUp = getInstanceUp(env, false);
				} catch (Exception ex) {
					success = 0;
					String msg = "OracleMonitor.execute method: calling resetConnection(false) and getInstanceUp(env) methods: '" + HelperUtils.getExceptionAsString(ex) + "'";
					log.severe(msg);
					result = new Status(StatusCode.PartialSuccess, msg, msg);
				}
			}
			
			if (isHistoryOn()) {
				historyInstanceUp = 0;
				try {
					historyInstanceUp = getInstanceUp(env, true);
					if (historyInstanceUp == 0) {
						resetConnection(true); // reset connection for a history database
						historyInstanceUp = getInstanceUp(env, true);
					}
				} catch(Exception e) {
					try {
						// sleep 2 seconds before resetting connection
						try {Thread.sleep(2);} catch (Exception e1){}
						resetConnection(true); // reset connection for a history database
						historyInstanceUp = getInstanceUp(env, true);
					} catch (Exception e1) {
						success = 0;
						String msg = "OracleMonitor.execute method: calling resetConnection(true) method: '" + HelperUtils.getExceptionAsString(e1) + "'";
						log.severe(msg);
						result = new Status(StatusCode.PartialSuccess, msg, msg);
					}
				}
			} else {
				historyInstanceUp = Double.NaN;
			}
			if (success == 1 && instanceUp == 1 && (historyInstanceUp == 1 || !isHistoryOn())) {
				result = super.execute(env);
				if (result != null) {
					if (result.getStatusCode().getBaseCode() == StatusCode.PartialSuccess.getBaseCode()) {
						success = 1;
					} else {
						success = 0;
						log.severe(result.getMessage());
						result = new Status(StatusCode.PartialSuccess, result.getShortMessage(), result.getMessage());
					}
				} else {
					String msg = "OracleMonitor.execute method: result is null";
					log.severe(msg);
					throw new RuntimeException(msg);
				}
			}
		} catch (Exception e) {
			success = 0;
			String msg = "OracleMonitor.execute method: outer try block: '" + HelperUtils.getExceptionAsString(e) + "'";
			log.severe(msg);
			result = new Status(StatusCode.PartialSuccess, msg, msg);
		}
		if (success == 0 && isHistoryOn()) {
			try {
				resetConnection(true); // reset connection for a history database
				historyInstanceUp = getInstanceUp(env, true);
			} catch (Exception e1) {
				historyInstanceUp = 0;
				String msg = "OracleMonitor.execute method: calling resetConnection(true) method before setting measures: '" + HelperUtils.getExceptionAsString(e1) + "'";
				log.severe(msg);
			}
		}
		try {
			if ((measures = env.getMonitorMeasures(SYSTEM_METRIC_GROUP, SUCCESS)) != null) {
				for (MonitorMeasure measure : measures) {
					log.finer("OracleMonitor.execute method: Populating SUCCESS...");
					measure.setValue(success);
				}
			}
		} catch (Exception e) {
			String msg = "OracleMonitor.execute method: calling measure.setValue(success) method: '" + HelperUtils.getExceptionAsString(e) + "'";
			log.severe(msg);
			return new Status(StatusCode.ErrorInfrastructure, msg, msg);
		}
		try {
			if ((measures = env.getMonitorMeasures(SYSTEM_METRIC_GROUP, INSTANCE_UP)) != null) {
				for (MonitorMeasure measure : measures) {
					log.finer("OracleMonitor.execute method: Populating INSTANCE_UP...");
					measure.setValue(instanceUp);
				}
			}
		} catch (Exception e) {
			String msg = "OracleMonitor.execute method: calling measure.setValue(instanceUp) method: '" + HelperUtils.getExceptionAsString(e) + "'";
			log.severe(msg);
			return new Status(StatusCode.ErrorInfrastructure, msg, msg);
		}
		try {
			if ((measures = env.getMonitorMeasures(SYSTEM_METRIC_GROUP, HISTORY_INSTANCE_UP)) != null) {
				for (MonitorMeasure measure : measures) {
					log.finer("OracleMonitor.execute method: Populating HISTORY_INSTANCE_UP...");
					measure.setValue(historyInstanceUp);
				}
			}
		} catch (Exception e) {
			String msg = "OracleMonitor.execute method: calling measure.setValue(instanceUp) method: '" + HelperUtils.getExceptionAsString(e) + "'";
			log.severe(msg);
			return new Status(StatusCode.ErrorInfrastructure, msg, msg);
		}
		
		return result;
	}

	@Override
	public void teardown(MonitorEnvironment env) throws Exception {
		super.teardown(env);
	}

}

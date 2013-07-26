package com.sjtu.omnilab.monitorservice;

import com.sjtu.omnilab.monitorbean.MonitorInfoBean;

/**
 * 
 * @author napu.zhang
 * @time 2013-07-17
 * Service for system monitor
 */
public interface MonitorService {

	public MonitorInfoBean getMonitorInfoBean() throws Exception;
}

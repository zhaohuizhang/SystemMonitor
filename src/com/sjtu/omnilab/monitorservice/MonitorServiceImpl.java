package com.sjtu.omnilab.monitorservice;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.lang.management.ManagementFactory;
import java.util.StringTokenizer;

import com.sjtu.omnilab.monitorbean.MonitorInfoBean;
import com.sjtu.omnilab.util.Bytes;
import com.sun.management.OperatingSystemMXBean;

public class MonitorServiceImpl implements MonitorService {

	private static final int CPUTIME = 30;
	private static final int PERCENT = 100;
	private static final int FAULTLENGTH = 10;
	private static String linuxVersion = null;
	
	public MonitorInfoBean getMonitorInfoBean() throws Exception {
		// TODO Auto-generated method stub
		int kb = 1024;
		long totalMemory = Runtime.getRuntime().totalMemory()/kb;
		long freeMemory = Runtime.getRuntime().freeMemory()/kb;
		long maxMemory = Runtime.getRuntime().maxMemory()/kb;
		OperatingSystemMXBean osmxb = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
		String osName = System.getProperty("os.name");
		long totalMemorySize = osmxb.getTotalPhysicalMemorySize()/kb;
		long freePhysicalMemorySize = osmxb.getFreePhysicalMemorySize() / kb;  
        long usedMemory = (osmxb.getTotalPhysicalMemorySize() - osmxb.getFreePhysicalMemorySize()) / kb;  
        ThreadGroup parentThread;  
        for (parentThread = Thread.currentThread().getThreadGroup(); parentThread.getParent() != null; parentThread = parentThread.getParent());  
        int totalThread = parentThread.activeCount();  
        double cpuRatio = 0;  
        if (osName.toLowerCase().startsWith("windows")) {  
            cpuRatio = this.getCpuRatioForWindows();  
        } else {  
            cpuRatio = getCpuRateForLinux();  
        } 
        MonitorInfoBean infoBean = new MonitorInfoBean();  
        infoBean.setFreeMemory(freeMemory);  
        infoBean.setFreePhysicalMemorySize(freePhysicalMemorySize);  
        infoBean.setMaxMemory(maxMemory);  
        infoBean.setOsName(osName);  
        infoBean.setTotalMemory(totalMemory);  
        infoBean.setTotalMemorySize(totalMemorySize);  
        infoBean.setTotalThread(totalThread);  
        infoBean.setUsedMemory(usedMemory);  
        infoBean.setCpuRatio(cpuRatio);  
        return infoBean; 
	}
	private static double getCpuRateForLinux() {  
        InputStream is = null;  
        InputStreamReader isr = null;  
        BufferedReader brStat = null;  
        StringTokenizer tokenStat = null;  
        try {  
            System.out.println("Get usage rate of CUP , linux version: " + linuxVersion);  
            Process process = Runtime.getRuntime().exec("top -b -n 1");  
            is = process.getInputStream();  
            isr = new InputStreamReader(is);  
            brStat = new BufferedReader(isr);  
            if (linuxVersion.equals("2.4")) {  
                brStat.readLine();  
                brStat.readLine();  
                brStat.readLine();  
                brStat.readLine();  
                tokenStat = new StringTokenizer(brStat.readLine());  
                tokenStat.nextToken();  
                tokenStat.nextToken();  
                String user = tokenStat.nextToken();  
                tokenStat.nextToken();  
                String system = tokenStat.nextToken();  
                tokenStat.nextToken();  
                String nice = tokenStat.nextToken();  
                System.out.println(user + " , " + system + " , " + nice);  
                user = user.substring(0, user.indexOf("%"));  
                system = system.substring(0, system.indexOf("%"));  
                nice = nice.substring(0, nice.indexOf("%"));  
                float userUsage = new Float(user).floatValue();  
                float systemUsage = new Float(system).floatValue();  
                float niceUsage = new Float(nice).floatValue();  
                return (userUsage + systemUsage + niceUsage) / 100;  
            } else {  
                brStat.readLine();  
                brStat.readLine();  
                tokenStat = new StringTokenizer(brStat.readLine());  
                tokenStat.nextToken();  
                tokenStat.nextToken();  
                tokenStat.nextToken();  
                tokenStat.nextToken();  
                tokenStat.nextToken();  
                tokenStat.nextToken();  
                tokenStat.nextToken();  
                String cpuUsage = tokenStat.nextToken();  
                System.out.println("CPU idle : " + cpuUsage);  
                Float usage = new Float(cpuUsage.substring(0, cpuUsage.indexOf("%")));  
                return (1 - usage.floatValue() / 100);  
            }  
        } catch (IOException ioe) {  
            System.out.println(ioe.getMessage());  
            freeResource(is, isr, brStat);  
            return 1;  
        } finally {  
            freeResource(is, isr, brStat);  
        }  
    }  
    private static void freeResource(InputStream is, InputStreamReader isr,  
            BufferedReader br) {  
        try {  
            if (is != null)  
                is.close();  
            if (isr != null)  
                isr.close();  
            if (br != null)  
                br.close();  
        } catch (IOException ioe) {  
            System.out.println(ioe.getMessage());  
        }  
    }  
    private double getCpuRatioForWindows() {  
        try {  
            String procCmd = System.getenv("windir") + "//system32//wbem//wmic.exe process get Caption,CommandLine,KernelModeTime,ReadOperationCount,ThreadCount,UserModeTime,WriteOperationCount";
            long[] c0 = readCpu(Runtime.getRuntime().exec(procCmd));  
            Thread.sleep(CPUTIME);  
            long[] c1 = readCpu(Runtime.getRuntime().exec(procCmd));  
            if (c0 != null && c1 != null) {  
                long idletime = c1[0] - c0[0];  
                long busytime = c1[1] - c0[1];  
                return Double.valueOf(PERCENT * (busytime) / (busytime + idletime)).doubleValue();  
            } else {  
                return 0.0;  
            }  
        } catch (Exception ex) {  
            ex.printStackTrace();  
            return 0.0;  
        }  
    }  
    private long[] readCpu(final Process proc) {  
        long[] retn = new long[2];  
        try {  
            proc.getOutputStream().close();  
            InputStreamReader ir = new InputStreamReader(proc.getInputStream());  
            LineNumberReader input = new LineNumberReader(ir);  
            String line = input.readLine();  
            if (line == null || line.length() < FAULTLENGTH) {  
                return null;  
            }  
            int capidx = line.indexOf("Caption");  
            int cmdidx = line.indexOf("CommandLine");  
            int rocidx = line.indexOf("ReadOperationCount");  
            int umtidx = line.indexOf("UserModeTime");  
            int kmtidx = line.indexOf("KernelModeTime");  
            int wocidx = line.indexOf("WriteOperationCount");  
            long idletime = 0;  
            long kneltime = 0;  
            long usertime = 0;  
            while ((line = input.readLine()) != null) {  
                if (line.length() < wocidx) {  
                    continue;  
                } 
                String caption = Bytes.substring(line, capidx, cmdidx - 1).trim();  
                String cmd = Bytes.substring(line, cmdidx, kmtidx - 1).trim();  
                if (cmd.indexOf("wmic.exe") >= 0) {  
                    continue;  
                }  
                String s1 = Bytes.substring(line, kmtidx, rocidx - 1).trim();  
                String s2 = Bytes.substring(line, umtidx, wocidx - 1).trim();  
                if (caption.equals("System Idle Process") || caption.equals("System")) {  
                    if (s1.length() > 0)  
                        idletime += Long.valueOf(s1).longValue();  
                    if (s2.length() > 0)  
                        idletime += Long.valueOf(s2).longValue();  
                    continue;  
                }  
                if (s1.length() > 0)  
                    kneltime += Long.valueOf(s1).longValue();  
                if (s2.length() > 0)  
                    usertime += Long.valueOf(s2).longValue();  
            }  
            retn[0] = idletime;  
            retn[1] = kneltime + usertime;  
            return retn;  
        } catch (Exception ex) {  
            ex.printStackTrace();  
        } finally {  
            try {  
                proc.getInputStream().close();  
            } catch (Exception e) {  
                e.printStackTrace();  
            }  
        }  
        return null;  
    }  
  
    public static void main(String[] args) throws Exception {  
        MonitorService service = new MonitorServiceImpl();  
        MonitorInfoBean monitorInfo = service.getMonitorInfoBean();  
        System.out.println("cpu占有率=" + monitorInfo.getCpuRatio());  
        System.out.println("可使用内存=" + monitorInfo.getTotalMemory());  
        System.out.println("剩余内存=" + monitorInfo.getFreeMemory());  
        System.out.println("最大可使用内存=" + monitorInfo.getMaxMemory());  
        System.out.println("操作系统=" + monitorInfo.getOsName());  
        System.out.println("总的物理内存=" + monitorInfo.getTotalMemorySize() + "kb");  
        System.out.println("剩余的物理内存=" + monitorInfo.getFreeMemory() + "kb");  
        System.out.println("已使用的物理内存=" + monitorInfo.getUsedMemory() + "kb");  
        System.out.println("线程总数=" + monitorInfo.getTotalThread() + "kb");  
    }  
	
	

}

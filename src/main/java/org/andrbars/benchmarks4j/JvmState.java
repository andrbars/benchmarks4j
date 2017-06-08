package org.andrbars.benchmarks4j;

import java.lang.management.ClassLoadingMXBean;
import java.lang.management.CompilationMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.lang.management.MemoryUsage;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class JvmState
{

	private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
	private static final DecimalFormat DECIMAL_FORMAT = new java.text.DecimalFormat("#0.000");
	private static final String SEPARATOR = "+---------------------+--------------------------------------------------------------+-----------------+-----------------+\n";
	private static final double GB = 1024 * 1024 * 1024;
	private static final double MB = 1024 * 1024;
	private static final double KB = 1024;

	public static String asString()
	{
		String timestamp = simpleDateFormat.format(new Date());

		StringBuilder log = new StringBuilder("\n");
		log.append(SEPARATOR);
		out(log, timestamp, "jvm state id", "raw value", "formatted value");
		log.append(SEPARATOR);

		RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
		outTime(log, timestamp, "Up time", runtimeMxBean.getUptime());
		log.append(SEPARATOR);

		CompilationMXBean compilation = ManagementFactory.getCompilationMXBean();
		outTime(log, timestamp, compilation.getName() + " total time", compilation.getTotalCompilationTime());
		log.append(SEPARATOR);

		ClassLoadingMXBean classLoading = ManagementFactory.getClassLoadingMXBean();
		outCount(log, timestamp, "Class loading (loaded class count)", classLoading.getLoadedClassCount());
		outCount(log, timestamp, "Class loading (total loaded class count)", classLoading.getTotalLoadedClassCount());
		outCount(log, timestamp, "Class loading (unloaded class count)", classLoading.getUnloadedClassCount());
		log.append(SEPARATOR);

		ThreadMXBean thread = ManagementFactory.getThreadMXBean();
		outCount(log, timestamp, "Thread daemon count", thread.getDaemonThreadCount());
		outCount(log, timestamp, "Thread count", thread.getThreadCount());
		outCount(log, timestamp, "Thread started count", thread.getTotalStartedThreadCount());
		log.append(SEPARATOR);

		for (MemoryPoolMXBean pool: ManagementFactory.getMemoryPoolMXBeans())
		{
			String name = pool.getName();
			MemoryType type = pool.getType();
			MemoryUsage usage = pool.getUsage();
			MemoryUsage peak = pool.getPeakUsage();
			outBytes(log, timestamp, type.toString() + " " + name + " uses", usage.getUsed());
			outBytes(log, timestamp, type.toString() + " " + name + " max", usage.getMax());
			outBytes(log, timestamp, type.toString() + " " + name + " peak", peak.getUsed());
			outBytes(log, timestamp, type.toString() + " " + name + " peak max", peak.getMax());
		}
		log.append(SEPARATOR);

		List<GarbageCollectorMXBean> gcmxb = ManagementFactory.getGarbageCollectorMXBeans();
		for (GarbageCollectorMXBean ob: gcmxb)
		{
			StringBuilder sb = new StringBuilder("GC ");
			sb.append(ob.getName()).append(" [");
			String[] str = ob.getMemoryPoolNames();
			for (int i = 0; i < str.length; i++)
			{
				sb.append(str[i].intern());
				if (i < str.length - 1)
				{
					sb.append(", ");
				}
			}
			sb.append("]");

			outTime(log, timestamp, sb.toString(), ob.getCollectionTime());
		}
		log.append(SEPARATOR);

		return log.toString();
	}

	private static void out(StringBuilder sb, String timeStamp, String id, String rawValue, String formattedValue)
	{
		sb.append(String.format("| %-19s | %-60s | %15s | %15s |", timeStamp, id, formattedValue, rawValue))
			.append("\n");
	}

	private static void outTime(StringBuilder sb, String timeStamp, String id, long rawValue)
	{
		out(sb, timeStamp, id, Long.toString(rawValue), (rawValue / 1000) + " seconds");
	}

	private static void outBytes(StringBuilder sb, String timeStamp, String id, long rawValue)
	{
		out(sb, timeStamp, id, Long.toString(rawValue), tidyFileSize(rawValue));
	}

	private static void outCount(StringBuilder sb, String timeStamp, String id, long rawValue)
	{
		out(sb, timeStamp, id, Long.toString(rawValue), Long.toString(rawValue));
	}

	public static String tidyFileSize(long size)
	{
		double calcSize;
		String str;
		if (size >= GB)
		{
			calcSize = size / GB;
			str = DECIMAL_FORMAT.format(calcSize) + " GB";
		}
		else if (size >= MB)
		{
			calcSize = size / MB;
			str = DECIMAL_FORMAT.format(calcSize) + " MB";
		}
		else if (size >= KB)
		{
			calcSize = size / KB;
			str = DECIMAL_FORMAT.format(calcSize) + " KB";
		}
		else
		{
			calcSize = size;
			str = DECIMAL_FORMAT.format(calcSize) + " B";
		}
		return str;
	}
}

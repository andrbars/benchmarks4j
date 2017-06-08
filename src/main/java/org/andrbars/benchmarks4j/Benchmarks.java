package org.andrbars.benchmarks4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class Benchmarks
{

	private static final Logger logger = LoggerFactory.getLogger(Benchmarks.class);
	private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
	private static final String SEPARATOR = "+---------------------+------------------------------------------------------------------------+------------+-------------+----------------+----------------+\n";
	private static final String HEADER = "| timestamp           | *id                                                                    |      count |  per second |   average time |     total time |\n";
	private static final String TOP_SEPARATOR_TREE = "+---------------------+------------------------------------------------------------------------+------------------------------------+-------------------------------------------+\n";
	private static final String HEADER_TREE = "| timestamp           | id                                                                     |            iterations              |                    time                   |\n";
	private static final String HEADER_SEPARATOR = "|                     |                                                                        +------------+---------+-------------+----------------+----------------+---------+\n";
	private static final String SUB_HEADER_TREE = "|                     |                                                                        |      count |       % |  per second |        average |          total |       % |\n";
	private static final String SEPARATOR_TREE = "+---------------------+------------------------------------------------------------------------+------------+---------+-------------+----------------+----------------+---------+\n";

	private static final Benchmark self = new Benchmark("[self]");

	private static final Map<String, Benchmark> items = new ConcurrentHashMap<>();
	// корень стека метрик
	private static Map<Benchmark, BenchmarkStack> stack = new ConcurrentHashMap<>();
	private static ThreadLocal<BenchmarkStack> currents = new ThreadLocal<>();

	private static long lastLogTime;
	private static boolean autoLogging = true;
	private static ArrayList<Benchmark> asStringBenchmarks = new ArrayList<>(256);
	private static ArrayList<BenchmarkStack> asStringTreeBenchmarks = new ArrayList<>(1024);
	private static ArrayList<BenchmarkStack> asStringTreeSubBenchmarks = new ArrayList<>(256);

	private static final Comparator<BenchmarkStack> stackComparator = new Comparator<BenchmarkStack>()
	{
		@Override
		public int compare(BenchmarkStack b1, BenchmarkStack b2)
		{
			int result = Integer.compare(b1.getLevel(), b2.getLevel());
			return result == 0
				? Long.compare(b2.getTotalTime(), b1.getTotalTime())
				: result;
		}
	};

	private static final Comparator<Benchmark> benchmarkComparator = new Comparator<Benchmark>()
	{
		@Override
		public int compare(Benchmark b1, Benchmark b2)
		{
			return Long.compare(b2.getTotalTime(), b1.getTotalTime());
		}
	};

	private Benchmarks()
	{
	}

	public static boolean has(String id)
	{
		return items.containsKey(id);
	}

	public static synchronized Benchmark get(String id)
	{
		Benchmark result = items.get(id);
		if (result == null)
		{
			result = new Benchmark(id);
			items.put(id, result);
		}
		return result;
	}

	public static Benchmark start(String id)
	{
		return start(get(id));
	}

	public static synchronized Benchmark start(Benchmark benchmark)
	{
		BenchmarkStack current = currents.get();
		Map<Benchmark, BenchmarkStack> s = current == null
			? stack
			: current.getStack();

		BenchmarkStack parent = current;
		current = s.get(benchmark);
		if (current == null)
		{
			current = new BenchmarkStack(benchmark, parent);
			s.put(benchmark, current);
		}

		currents.set(current);
		current.start();

		return benchmark;
	}

	public static synchronized Benchmark stop(Benchmark benchmark)
	{
		BenchmarkStack current = currents.get();
		if ((current == null) || (current.getBenchmark() != benchmark))
		{
			return benchmark;
		}

		current.stop();
		currents.set(current.getParent());

		long time = System.currentTimeMillis();
		if (autoLogging && (time - lastLogTime > 3 * 60 * 1000) && (current.getParent() == null))
		{// раз в 3 минуты
			lastLogTime = time;
			String report = JvmState.asString() + "\n"
				+ asString(true) + "\n"
				+ asStringTree(true);
			logger.info(report);
		}

		return benchmark;
	}

	public static synchronized Benchmark stop(String id)
	{
		return stop(get(id));
	}

	public static synchronized Benchmark cancel(String id)
	{
		return cancel(get(id));
	}

	public static synchronized Benchmark cancel(Benchmark benchmark)
	{
		benchmark.cancel();
		return benchmark;
	}

	public static synchronized Benchmark clear(String id)
	{
		return clear(get(id));
	}

	public static synchronized Benchmark clear(Benchmark benchmark)
	{
		benchmark.clear();
		return benchmark;
	}

	public static synchronized void clear()
	{
		for (Benchmark benchmark: items.values())
		{
			benchmark.clear();
		}
	}

	public static Collection<Benchmark> list()
	{
		return items.values();
	}

	/**
	 * Формирование таблицы замеренных показателей
	 *
	 * @param printEmpty true - выводить записи, у которых count=0
	 *
	 * @return таблица замеренных показателей
	 */
	public static synchronized String asString(boolean printEmpty)
	{
		String timestamp = simpleDateFormat.format(new Date());
		StringBuilder sb = new StringBuilder("\n");
		sb.append(SEPARATOR);
		sb.append(HEADER);
		sb.append(SEPARATOR);
		int i = 0;

		// сортировка записей в порядке убывания общего времени метрики
		asStringBenchmarks.clear();
		asStringBenchmarks.addAll(items.values());
		Collections.sort(asStringBenchmarks, benchmarkComparator);

		for (Benchmark item: asStringBenchmarks)
		{
			if (!printEmpty && (item.getCount() == 0))
			{
				continue;
			}

			sb.append(item.asString(timestamp)).append("\n");

			i++;
			if (i % 4 == 0)
			{
				sb.append(SEPARATOR);
			}
		}
		if (i % 4 != 0)
		{
			sb.append(SEPARATOR);
		}
		return sb.toString();
	}

	public static synchronized String asStringTree(boolean printEmpty)
	{
		String timestamp = simpleDateFormat.format(new Date());
		StringBuilder sb = new StringBuilder("\n");
		sb.append(TOP_SEPARATOR_TREE);
		sb.append(HEADER_TREE);
		sb.append(HEADER_SEPARATOR);
		sb.append(SUB_HEADER_TREE);
		sb.append(SEPARATOR_TREE);

		asStringTreeBenchmarks.clear();
		asStringTreeBenchmarks.addAll(stack.values());
		// сортировка записей в порядке убывания общего времени метрики
		Collections.sort(asStringTreeBenchmarks, stackComparator);
		for (int i = 0; i < asStringTreeBenchmarks.size(); i++)
		{
			BenchmarkStack current = asStringTreeBenchmarks.get(i);
			if (current.getBenchmark() == self)
			{
				continue;
			}

			asStringTreeSubBenchmarks.clear();
			Collection<BenchmarkStack> nestedItems = current.getStack().values();
			if (!nestedItems.isEmpty())
			{
				// добавление собственного времени
				long totalTime = 0;
				for (BenchmarkStack nestedItem: nestedItems)
				{
					totalTime += nestedItem.getTotalTime();
				}
				totalTime = current.getTotalTime() - totalTime;
				if (totalTime > 0)
				{
					BenchmarkStack selfStack = new BenchmarkStack(self, current);
					selfStack.update(current.getCount(), totalTime);
					asStringTreeSubBenchmarks.add(selfStack);
				}

				asStringTreeSubBenchmarks.addAll(nestedItems);
				// сортировка вложенных записей в порядке убывания общего времени метрики
				Collections.sort(asStringTreeSubBenchmarks, stackComparator);
				asStringTreeBenchmarks.addAll(i + 1, asStringTreeSubBenchmarks);
			}
		}

		for (BenchmarkStack item: asStringTreeBenchmarks)
		{
			if (!printEmpty && (item.getCount() == 0))
			{
				continue;
			}

			sb.append(item.asString(timestamp)).append("\n");
		}
		sb.append(SEPARATOR_TREE);

		return sb.toString();
	}

	public static boolean isAutoLogging()
	{
		return autoLogging;
	}

	public static void setAutoLogging(boolean autoLogging)
	{
		Benchmarks.autoLogging = autoLogging;
	}
}

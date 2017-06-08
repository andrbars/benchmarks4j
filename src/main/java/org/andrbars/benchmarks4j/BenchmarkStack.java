package org.andrbars.benchmarks4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Дерево измерений показателя
 */
public class BenchmarkStack extends BenchmarkExecution
{

	/**
	 * Измеряемый показатель
	 */
	private final Benchmark benchmark;
	/**
	 * Кратность запусков замера показателя
	 */
	private final AtomicLong count = new AtomicLong(0);
	/**
	 * Общее время всех запусков
	 */
	private final AtomicLong totalTime = new AtomicLong(0);

	private final BenchmarkStack parent;
	private int level;
	// ветки дерева стека метрик
	private final Map<Benchmark, BenchmarkStack> stack = new ConcurrentHashMap<>();

	BenchmarkStack(Benchmark benchmark, BenchmarkStack parent)
	{
		this.benchmark = benchmark;
		this.parent = parent;
		this.level = parent != null
			? parent.level + 1
			: 0;
	}

	@Override
	long stop()
	{
		long elapsed = super.stop();
		count.incrementAndGet();
		totalTime.addAndGet(elapsed);
		benchmark.update(elapsed);
		return elapsed;
	}

	public Benchmark getBenchmark()
	{
		return benchmark;
	}

	public BenchmarkStack getParent()
	{
		return parent;
	}

	Map<Benchmark, BenchmarkStack> getStack()
	{
		return stack;
	}

	int getLevel()
	{
		return level;
	}

	long getTotalTime()
	{
		return totalTime.get();
	}

	long getCount()
	{
		return count.get();
	}

	public double getPerSecond()
	{
		return getAverageTime() == 0
			? 0
			: 1e9 / getAverageTime();
	}

	public long getAverageTime()
	{
		return count.get() == 0
			? 0
			: totalTime.get() / count.get();
	}

	public void update(long count, long totalTime)
	{
		this.totalTime.set(totalTime);
		this.count.set(count);
	}

	public String asString(String timeStamp)
	{
		String gap = level == 0
			? ""
			: String.format("%-" + level + "s", " ");
		double countPercent = parent == null
			? 100
			: count.get() * 100 / (double)parent.getCount();
		double totalTimePercent = parent == null
			? 100
			: totalTime.get() * 100 / (double)parent.getTotalTime();

		return String.format("| %-17s | %-70s | %10d | %7.2f | %11.3f | %14.6f | %14.6f | %7.2f |",
			timeStamp, gap + benchmark.getId(), count.get(), countPercent, getPerSecond(), getAverageTime() / 1e9,
			totalTime.get() / 1e9, totalTimePercent);
	}

}

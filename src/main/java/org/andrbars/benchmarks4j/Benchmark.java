package org.andrbars.benchmarks4j;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Измеряемый показатель
 */
public class Benchmark
{

	private final ThreadLocal<BenchmarkExecution> items = new ThreadLocal<>();

	/**
	 * Идентификатор показателя
	 */
	private final String id;
	/**
	 * Кратность запусков замера показателя
	 */
	private final AtomicLong count = new AtomicLong(0);
	/**
	 * Общее время всех запусков
	 */
	private final AtomicLong totalTime = new AtomicLong(0);

	public Benchmark(String id)
	{
		this.id = id;
	}

	public void start()
	{
		BenchmarkExecution item = items.get();
		if (item == null)
		{
			item = new BenchmarkExecution();
			items.set(item);
		}
		item.start();
	}

	public long stop()
	{
		BenchmarkExecution item = items.get();
		if (item == null)
		{
			return 0;
		}

		long elapsed = item.stop();
		update(elapsed);
		return elapsed;
	}

	void update(long elapsed)
	{
		count.incrementAndGet();
		totalTime.addAndGet(elapsed);
	}

	public void cancel()
	{
		BenchmarkExecution item = items.get();
		if (item != null)
		{
			item.stop();
		}
	}

	public void clear()
	{
		count.set(0);
		totalTime.set(0);
	}

	public String getId()
	{
		return id;
	}

	public long getCount()
	{
		return count.get();
	}

	public long getTotalTime()
	{
		return totalTime.get();
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

	public String asString(String timeStamp)
	{
		return String.format("| %-17s | *%-69s | %10d | %11.3f | %14.6f | %14.6f |",
			timeStamp, id, count.get(), getPerSecond(), getAverageTime() / 1e9, totalTime.get() / 1e9);
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o)
		{
			return true;
		}
		if (o == null || getClass() != o.getClass())
		{
			return false;
		}

		Benchmark benchmark = (Benchmark)o;

		return id.equals(benchmark.id);

	}

	@Override
	public int hashCode()
	{
		return id.hashCode();
	}
}

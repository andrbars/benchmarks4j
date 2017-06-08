package org.andrbars.benchmarks4j;

class BenchmarkExecution
{

	/**
	 * Время запуска текущего замера или 0, если замер в настоящее время не производится
	 */
	private long startedAt;

	void start()
	{
		if (startedAt == 0)
		{
			startedAt = System.nanoTime();
		}
	}

	long stop()
	{
		if (startedAt != 0)
		{
			long elapsed = System.nanoTime() - startedAt;
			startedAt = 0;
			return elapsed;
		}
		return 0;
	}
}

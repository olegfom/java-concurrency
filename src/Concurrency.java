import java.io.*;
import java.math.*;
import java.security.*;
import java.text.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAccumulator;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.StampedLock;
import java.util.function.*;
import java.util.regex.*;
import java.util.stream.*;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

public class Concurrency {

	public static Integer count=0;
	
	public static void main(String[] args) {
		
		simpleRunnable();
		runnableCallableShutdown();
		invokeAll();
		invokeAny();
		delay_scheduler();
		frequent_scheduler_fixedRate();
		frequent_scheduler_fixedDelay();
		increment_sync();
		reentrantLock();
		readWriteLock();
		optimisticReadStampedLock();
		stampedLockConvertToWriteLock();
		semaphores();
		atomicOperations();
		longAdderAndAccumulator();


	}

	public static void simpleRunnable( ) {
		
		System.out.println("\nMethod: " +
	            Thread.currentThread().getStackTrace()[1].getMethodName() + "\n-------------------");
		
			Runnable task = () -> {
		    String threadName = Thread.currentThread().getName();
		    System.out.println("Hello " + threadName);		    
		};
	
		task.run();
	
		
		Thread thread = new Thread(task);
		thread.start();
	
		System.out.println("Done!");
		
	}
	
	public static void runnableCallableShutdown( ) {
		System.out.println("\nMethod: " +
	            Thread.currentThread().getStackTrace()[1].getMethodName() + "\n-------------------");

		ExecutorService executor = Executors.newSingleThreadExecutor();
		//Runnable task
		executor.submit(() -> {
			    String threadName = Thread.currentThread().getName();
			    System.out.println("Hello Runnable " + threadName);
			});

		//Callable task
		Future<Integer> future = executor.submit(() -> {
			    try {
			    	String threadName = Thread.currentThread().getName();
				    System.out.println("Hello Callable" + threadName);
			        TimeUnit.SECONDS.sleep(1);
			        return 123;
			    }
			    catch (InterruptedException e) {
			        throw new IllegalStateException("task interrupted", e);
			    }
			});		
		System.out.println("future done? " + future.isDone());

		Integer result=-1;
		try {
			System.out.println("Getting the future with a timeout");
			result = future.get(3, TimeUnit.SECONDS);
		} catch (InterruptedException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		} catch (TimeoutException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		} catch (ExecutionException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}

		System.out.println("future done? " + future.isDone());
		System.out.println("result: " + result);
		
		
		try {
			TimeUnit.SECONDS.sleep(3);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		try {
		    System.out.println("attempt to shutdown executor");
		    executor.shutdown();
		    executor.awaitTermination(5, TimeUnit.SECONDS);
		}
		catch (InterruptedException e) {
		    System.err.println("tasks interrupted");
		}
		finally {
		    if (!executor.isTerminated()) {
		        System.err.println("cancel non-finished tasks");
		    }
		    executor.shutdownNow();
		    System.out.println("shutdown finished");
		}
		
	}

	public static void invokeAll( ) {
		System.out.println("\nMethod: " +
	            Thread.currentThread().getStackTrace()[1].getMethodName() + "\n-------------------");

		ExecutorService executor = Executors.newWorkStealingPool();

		List<Callable<String>> callables = Arrays.asList(
		        () -> { TimeUnit.SECONDS.sleep(5); return "task1";},
		        () -> { TimeUnit.SECONDS.sleep(3); return "task2";},
		        () -> { TimeUnit.SECONDS.sleep(1); return "task3";});

		try {
			executor.invokeAll(callables)
			    .stream()
			    .map(future -> {
			        try {
			            return future.get();
			        }
			        catch (Exception e) {
			            throw new IllegalStateException(e);
			        }
			    })
			    .forEach(System.out::println);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}

	public static Callable<String> callable (String result, long sleepSeconds) {
	    return () -> {
	        TimeUnit.SECONDS.sleep(sleepSeconds);
	        return result;
	    };
	}
	
	public static void invokeAny( ) {
		System.out.println("\nMethod: " +
	            Thread.currentThread().getStackTrace()[1].getMethodName() + "\n-------------------");
		
		ExecutorService executor = Executors.newWorkStealingPool();

		List<Callable<String>> callables = Arrays.asList(
		    callable("task1", 2),
		    callable("task2", 1),
		    callable("task3", 3));

		String result="";
		try {
			result = executor.invokeAny(callables);
		} catch (InterruptedException | ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println(result);
	}
	
	public static void delay_scheduler( ) {
		System.out.println("\nMethod: " +
	            Thread.currentThread().getStackTrace()[1].getMethodName() + "\n-------------------");
		
		ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

		Runnable task = () -> System.out.println("I'm done: " + System.nanoTime());
		
		ScheduledFuture<?> future = executor.schedule(task, 5, TimeUnit.SECONDS);

		try {
			TimeUnit.MILLISECONDS.sleep(1337);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		long remainingDelay = future.getDelay(TimeUnit.MILLISECONDS);
		System.out.printf("Remaining Delay: %sms\n", remainingDelay);
		try {
			future.get();
		} catch (InterruptedException | ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	public static void frequent_scheduler_fixedRate() {
		System.out.println("\nMethod: " +
	            Thread.currentThread().getStackTrace()[1].getMethodName() + "\n-------------------");
		
		ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

		Runnable task = () -> {
				System.out.println("Scheduling: " + System.nanoTime());
			};

		try {
			int initialDelay = 0;
			int period = 2;
			Future future = executor.scheduleAtFixedRate(task, initialDelay, period, TimeUnit.SECONDS);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			TimeUnit.SECONDS.sleep(10);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		try {
		    System.out.println("attempt to shutdown executor");
		    executor.shutdown();
		    executor.awaitTermination(5, TimeUnit.SECONDS);
		}
		catch (InterruptedException e) {
		    System.err.println("tasks interrupted");
		}
		finally {
		    if (!executor.isTerminated()) {
		        System.err.println("cancel non-finished tasks");
		    }
		    executor.shutdownNow();
		    System.out.println("shutdown finished");
		}
	}
	
	public static void frequent_scheduler_fixedDelay() {
		System.out.println("\nMethod: " +
	            Thread.currentThread().getStackTrace()[1].getMethodName() + "\n-------------------");
		
		ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

		Runnable task = () -> {
			    try {
			        TimeUnit.SECONDS.sleep(2);
			        System.out.println("Scheduling: " + System.nanoTime());
			    }
			    catch (InterruptedException e) {
			        System.err.println("task interrupted");
			    }
			};

		try {
			int initialDelay = 0;
			int period = 2;
			Future future = executor.scheduleWithFixedDelay(task, initialDelay, period, TimeUnit.SECONDS);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			TimeUnit.SECONDS.sleep(10);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		stop(executor);
	}
	
	public static void stop(ExecutorService executor) {
		try {
		    System.out.println("attempt to stop executor");
		    executor.shutdown();
		    executor.awaitTermination(5, TimeUnit.SECONDS);
		}
		catch (InterruptedException e) {
		    System.err.println("tasks interrupted");
		}
		finally {
		    if (!executor.isTerminated()) {
		        System.err.println("cancel non-finished tasks");
		    }
		    executor.shutdownNow();
		    System.out.println("executor stopped");
		}		
	}
	
    public static void sleep(int seconds) {
        try {
            TimeUnit.SECONDS.sleep(seconds);
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }
    
    synchronized public static void increment() {
				count = count + 1;
	};

	public static void increment_sync() {

		System.out.println("\nMethod: " +
	            Thread.currentThread().getStackTrace()[1].getMethodName() + "\n-------------------");

		ExecutorService executor = Executors.newFixedThreadPool(10);

		IntStream.range(0, 10000)
		    .forEach(i -> executor.submit(Concurrency::increment));

		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("count =" + count);  // 9965
		
		stop(executor);
	}
	
	public static void reentrantLock() {
		
		System.out.println("\nMethod: " +
	            Thread.currentThread().getStackTrace()[1].getMethodName() + "\n-------------------");

		ExecutorService executor = Executors.newFixedThreadPool(10);
		ReentrantLock lock = new ReentrantLock();
		
		executor.submit(() -> {
		    lock.lock();
		    try {
		        sleep(1);
		    } finally {
		        lock.unlock();
		    }
		});

		executor.submit(() -> {
		    System.out.println("Locked: " + lock.isLocked());
		    System.out.println("Held by me: " + lock.isHeldByCurrentThread());
		    sleep(2);
		    boolean locked = lock.tryLock();
		    System.out.println("Lock acquired: " + locked);
		});

		stop(executor);		
	}
	
	public static void readWriteLock() {
		System.out.println("\nMethod: " +
	            Thread.currentThread().getStackTrace()[1].getMethodName() + "\n-------------------");

		ExecutorService executor = Executors.newFixedThreadPool(3);
		Map<String, String> map = new HashMap<>();
		ReadWriteLock lock = new ReentrantReadWriteLock();

		executor.submit(() -> {
		    lock.writeLock().lock();
		    try {
		        sleep(1);
		        map.put("foo", "bar");
		    } finally {
		        lock.writeLock().unlock();
		    }
		});
		
		Runnable readTask = () -> {
		    lock.readLock().lock();
		    try {
			        System.out.println(map.get("foo"));
		    } finally {
		        lock.readLock().unlock();
		    }
		};

		sleep(2);
		
		executor.submit(readTask);
		executor.submit(readTask);
		
		stop(executor);		
	}
	
	public static void optimisticReadStampedLock() {
		System.out.println("\nMethod: " +
	            Thread.currentThread().getStackTrace()[1].getMethodName() + "\n-------------------");

		ExecutorService executor = Executors.newFixedThreadPool(2);
		StampedLock lock = new StampedLock();

		executor.submit(() -> {
		    long stamp = lock.tryOptimisticRead();
		    try {
		        System.out.println("Optimistic Lock Valid: " + lock.validate(stamp));
		        sleep(1);
		        System.out.println("Optimistic Lock Valid: " + lock.validate(stamp));
		        sleep(2);
		        System.out.println("Optimistic Lock Valid: " + lock.validate(stamp));
		    } finally {
		        lock.unlock(stamp);
		    }
		});

		executor.submit(() -> {
		    long stamp = lock.writeLock();
		    try {
		        System.out.println("Write Lock acquired");
		        sleep(2);
		    } finally {
		        lock.unlock(stamp);
		        System.out.println("Write done");
		    }
		});

		sleep(4);
		stop(executor);		
	}
	
	public static void stampedLockConvertToWriteLock() {
		System.out.println("\nMethod: " +
	            Thread.currentThread().getStackTrace()[1].getMethodName() + "\n-------------------");
		ExecutorService executor = Executors.newFixedThreadPool(2);
		StampedLock lock = new StampedLock();

		executor.submit(() -> {
		    long stamp = lock.readLock();
		    try {
		        if (count == 0) {
		            stamp = lock.tryConvertToWriteLock(stamp);
		            if (stamp == 0L) {
		                System.out.println("Could not convert to write lock");
		                stamp = lock.writeLock();
		            }
		            count = 23;
		        }
		        System.out.println(count);
		    } finally {
		        lock.unlock(stamp);
		    }
		});

		sleep(1);
		stop(executor);				
	}
	
	public static void semaphores() {
		
		System.out.println("\nMethod: " +
	            Thread.currentThread().getStackTrace()[1].getMethodName() + "\n-------------------");
		ExecutorService executor = Executors.newFixedThreadPool(10);

		Semaphore semaphore = new Semaphore(5);

		Runnable longRunningTask = () -> {
		    boolean permit = false;
		    try {
		        permit = semaphore.tryAcquire(1, TimeUnit.SECONDS);
		        if (permit) {
		            System.out.println("Semaphore acquired");
		            sleep(5);
		        } else {
		            System.out.println("Could not acquire semaphore");
		        }
		    } catch (InterruptedException e) {
		        throw new IllegalStateException(e);
		    } finally {
		        if (permit) {
		            semaphore.release();
		        }
		    }
		};

		IntStream.range(0, 10)
		    .forEach(i -> executor.submit(longRunningTask));

		sleep(7);
		stop(executor);
	}
	
	public static void atomicOperations() {
	
		System.out.println("\nMethod: " +
	            Thread.currentThread().getStackTrace()[1].getMethodName() + "\n-------------------");
		AtomicInteger atomicInt = new AtomicInteger(0);

		ExecutorService executor = Executors.newFixedThreadPool(2);

		IntStream.range(0, 1000)
		    .forEach(i -> executor.submit(atomicInt::incrementAndGet));


		sleep(1);
		System.out.println(atomicInt.get());    // =>
		stop(executor);

		
		AtomicInteger atomicInt1 = new AtomicInteger(0);

		ExecutorService executor1 = Executors.newFixedThreadPool(2);

		IntStream.range(0, 1000)
		    .forEach(i -> {
		        Runnable task = () -> atomicInt1.updateAndGet(n -> n + 2);
		        executor1.submit(task);
		    });

		sleep(1);
		System.out.println(atomicInt1.get());    // => 2000
		stop(executor1);
		
		AtomicInteger atomicInt2 = new AtomicInteger(0);

		ExecutorService executor2 = Executors.newFixedThreadPool(2);

		IntStream.range(0, 1000)
	    .forEach(i -> {
	        Runnable task = () ->
	            atomicInt2.accumulateAndGet(i, (n, m) -> n + m);
	        executor2.submit(task);
	    });

		sleep(1);
		System.out.println(atomicInt2.get());    // => 499500
		stop(executor2);
		
	}
	
	public static void longAdderAndAccumulator() {
		System.out.println("\nMethod: " +
	            Thread.currentThread().getStackTrace()[1].getMethodName() + "\n-------------------");
		LongAdder adder = new LongAdder();
		ExecutorService executor = Executors.newFixedThreadPool(2);
		
		IntStream.range(0,10000)
		.forEach(i -> executor.submit(adder::increment));

		sleep(1);
		System.out.println(adder.sumThenReset());   // => 1000
		stop(executor);
		//-----------------------
		
		LongBinaryOperator op = (x, y) -> 2 * x + y;
		LongAccumulator accumulator = new LongAccumulator(op, 1L);

		ExecutorService executor1 = Executors.newFixedThreadPool(1);

		IntStream.range(0, 10)
		    .forEach(i -> executor1.submit(() -> {
		    		accumulator.accumulate(i);
		    		System.out.println(i + ":" +accumulator);
		    	}));

		sleep(1);
		System.out.println(accumulator.getThenReset());     // => 2539
		stop(executor1);
		
	}
	
}

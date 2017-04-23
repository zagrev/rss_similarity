/**
 *
 */
package com.minethurn.cs7210.rss_similarity;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author Steve Betts
 */
public class BlockingFixedThreadPoolExecutor extends ThreadPoolExecutor
{
   private final Semaphore semaphore;

   /**
    * Create a block executor with the given number of threads and the max size of the queue
    *
    * @param threads
    *        The number of threads to create
    * @param bound
    *        the maximum size of the queue
    */
   public BlockingFixedThreadPoolExecutor(final int threads, final int bound)
   {
      super(threads, threads, 1, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
      this.semaphore = new Semaphore(bound);
   }

   @Override
   protected void afterExecute(final Runnable r, final Throwable t)
   {
      semaphore.release();
   }

   @Override
   public void execute(final Runnable task)
   {
      semaphore.acquireUninterruptibly();

      try
      {
         super.execute(task);
      }
      catch (final Throwable t)
      {
         semaphore.release();
         throw new RuntimeException(t);
      }
   }

   /**
    * get the number of tasks in the queue. {@link Queue#size()}
    *
    * @return the depth of the queue
    */
   public int getSize()
   {
      return ((LinkedBlockingQueue<Runnable>) getQueue()).size();
   }
}

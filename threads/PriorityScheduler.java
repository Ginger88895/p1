package nachos.threads;

import nachos.machine.*;

import java.lang.Math;
import java.util.TreeSet;
import java.util.HashSet;
import java.util.Iterator;

/**
 * A scheduler that chooses threads based on their priorities.
 *
 * <p>
 * A priority scheduler associates a priority with each thread. The next thread
 * to be dequeued is always a thread with priority no less than any other
 * waiting thread's priority. Like a round-robin scheduler, the thread that is
 * dequeued is, among all the threads of the same (highest) priority, the
 * thread that has been waiting longest.
 *
 * <p>
 * Essentially, a priority scheduler gives access in a round-robin fassion to
 * all the highest-priority threads, and ignores all other threads. This has
 * the potential to
 * starve a thread if there's always a thread waiting with higher priority.
 *
 * <p>
 * A priority scheduler must partially solve the priority inversion problem; in
 * particular, priority must be donated through locks, and through joins.
 */
public class PriorityScheduler extends Scheduler
{
    /**
     * Allocate a new priority scheduler.
     */
    public PriorityScheduler() {
    }
    
    /**
     * Allocate a new priority thread queue.
     *
     * @param	transferPriority	<tt>true</tt> if this queue should
     *					transfer priority from waiting threads
     *					to the owning thread.
     * @return	a new priority thread queue.
     */
    public ThreadQueue newThreadQueue(boolean transferPriority) {
	return new PriorityQueue(transferPriority);
    }

    public int getPriority(KThread thread) {
	Lib.assertTrue(Machine.interrupt().disabled());
		       
	return getThreadState(thread).getPriority();
    }

    public int getEffectivePriority(KThread thread) {
	Lib.assertTrue(Machine.interrupt().disabled());
		       
	return getThreadState(thread).getEffectivePriority();
    }

    public void setPriority(KThread thread, int priority) {
	Lib.assertTrue(Machine.interrupt().disabled());
		       
	Lib.assertTrue(priority >= priorityMinimum &&
		   priority <= priorityMaximum);
	
	getThreadState(thread).setPriority(priority);
    }

    public boolean increasePriority() {
	boolean intStatus = Machine.interrupt().disable();
		       
	KThread thread = KThread.currentThread();

	int priority = getPriority(thread);
	if (priority == priorityMaximum)
	    return false;

	setPriority(thread, priority+1);

	Machine.interrupt().restore(intStatus);
	return true;
    }

    public boolean decreasePriority() {
	boolean intStatus = Machine.interrupt().disable();
		       
	KThread thread = KThread.currentThread();

	int priority = getPriority(thread);
	if (priority == priorityMinimum)
	    return false;

	setPriority(thread, priority-1);

	Machine.interrupt().restore(intStatus);
	return true;
    }

    /**
     * The default priority for a new thread. Do not change this value.
     */
    public static final int priorityDefault = 1;
    /**
     * The minimum priority that a thread can have. Do not change this value.
     */
    public static final int priorityMinimum = 0;
    /**
     * The maximum priority that a thread can have. Do not change this value.
     */
    public static final int priorityMaximum = 7;    

    /**
     * Return the scheduling state of the specified thread.
     *
     * @param	thread	the thread whose scheduling state to return.
     * @return	the scheduling state of the specified thread.
     */
    protected ThreadState getThreadState(KThread thread) {
	if (thread.schedulingState == null)
	    thread.schedulingState = new ThreadState(thread);

	return (ThreadState) thread.schedulingState;
    }

    /**
     * A <tt>ThreadQueue</tt> that sorts threads by priority.
     */
    protected class PriorityQueue extends ThreadQueue
	{
		PriorityQueue(boolean transferPriority)
		{
			this.transferPriority = transferPriority;
			this.waitingPriority=-1;
			this.valid=false;
			this.waitQueue=new HashSet<ThreadState>();
		}

		public void waitForAccess(KThread thread)
		{
			/*
			System.out.println("Before wait:");
			print();
			*/
			Lib.assertTrue(Machine.interrupt().disabled());
			waitQueue.add(getThreadState(thread));
			this.valid=false;
			getThreadState(thread).waitForAccess(this);
			/*
			System.out.println("After wait:");
			print();
			*/
		}

		public void acquire(KThread thread)
		{
			/*
			System.out.println("Before acquire:");
			print();
			*/
			Lib.assertTrue(Machine.interrupt().disabled());
			acquiringThread=getThreadState(thread);
			getThreadState(thread).acquire(this);
			/*
			System.out.println("After acquire:");
			print();
			*/
		}

		public KThread nextThread()
		{
			/*
			System.out.println("Before next:");
			print();
			*/
			Lib.assertTrue(Machine.interrupt().disabled());
			if(waitQueue.isEmpty())
			{
				if(transferPriority)
				{
					acquiringThread.acquiredQueues.remove(this);
					this.valid=false;
					acquiringThread.valid=false;
				}
				acquiringThread=null;
				/*
				System.out.println("After next:");
				print();
				*/
				return null;
			}
			else
			{
				acquiringThread=pickNextThread();
				if(transferPriority)
				{
					acquiringThread.acquiredQueues.remove(this);
					this.valid=false;
					acquiringThread.valid=false;
				}
				waitQueue.remove(acquiringThread);
				acquiringThread.acquire(this);
				/*
				System.out.println("After next:");
				print();
				*/
				return acquiringThread.thread;
			}
		}

		/**
		 * Return the next thread that <tt>nextThread()</tt> would return,
		 * without modifying the state of this queue.
		 *
		 * @return	the next thread that <tt>nextThread()</tt> would
		 *		return.
		 */
		protected ThreadState pickNextThread()
		{
			// implement me
			if(waitQueue.isEmpty())
				return null;
			else
			{
				boolean found=false;
				int maxEff=0;
				long minTime=0;
				ThreadState next=null;
				for(ThreadState x:waitQueue)
				{
					if(!found||x.getEffectivePriority()>maxEff||(x.getEffectivePriority()==maxEff&&x.waitTime<minTime))
					{
						found=true;
						maxEff=x.getEffectivePriority();
						minTime=x.waitTime;
						next=x;
					}
				}
				return next;
			}
		}
		
		public void print()
		{
			Lib.assertTrue(Machine.interrupt().disabled());
			if(acquiringThread!=null)
				System.out.println("Acquired Thread: "+acquiringThread.thread);
			else
				System.out.println("Acquired Thread: null");
			for(ThreadState x:waitQueue)
				System.out.println(x.thread);
		}

		public int getEffectivePriority()
		{
			/*
			System.out.println("getEffectivePriority called on queue "+this);
			print();
			*/
			if(!valid)
			{
				waitingPriority=-1;
				for(ThreadState x:waitQueue)
					waitingPriority=Math.max(waitingPriority,x.getEffectivePriority());
				valid=true;
			}
			return waitingPriority;
		}

		/**
		 * <tt>true</tt> if this queue should transfer priority from waiting
		 * threads to the owning thread.
		 */
		public boolean transferPriority;
		protected int waitingPriority;
		protected boolean valid;
		protected ThreadState acquiringThread=null;
		protected HashSet<ThreadState> waitQueue;
    }

    /**
     * The scheduling state of a thread. This should include the thread's
     * priority, its effective priority, any objects it owns, and the queue
     * it's waiting for, if any.
     *
     * @see	nachos.threads.KThread#schedulingState
     */
    protected class ThreadState
	{
		/**
		 * Allocate a new <tt>ThreadState</tt> object and associate it with the
		 * specified thread.
		 *
		 * @param	thread	the thread this state belongs to.
		 */
		public ThreadState(KThread thread)
		{
			this.thread=thread;
			this.valid=false;
			this.acquiredQueues=new HashSet<PriorityQueue>();
			this.waitingQueues=new HashSet<PriorityQueue>();
			setPriority(priorityDefault);
		}

		/**
		 * Return the priority of the associated thread.
		 *
		 * @return	the priority of the associated thread.
		 */
		public int getPriority()
		{
			return priority;
		}

		/**
		 * Return the effective priority of the associated thread.
		 *
		 * @return	the effective priority of the associated thread.
		 */
		public int getEffectivePriority()
		{
			//System.out.println("getEffectivePriority called on thread "+thread);
			if(!valid)
			{
				effectivePriority=priority;
				for(PriorityQueue x:acquiredQueues)
					effectivePriority=Math.max(effectivePriority,x.getEffectivePriority());
				valid=true;
			}
			return effectivePriority;
		}

		/**
		 * Set the priority of the associated thread to the specified value.
		 *
		 * @param	priority	the new priority.
		 */
		public void setPriority(int priority)
		{
			if (this.priority == priority)
			return;
			
			this.priority = priority;
			this.valid=false;
			for(PriorityQueue x:waitingQueues)
				x.valid=false;
		}

		/**
		 * Called when <tt>waitForAccess(thread)</tt> (where <tt>thread</tt> is
		 * the associated thread) is invoked on the specified priority queue.
		 * The associated thread is therefore waiting for access to the
		 * resource guarded by <tt>waitQueue</tt>. This method is only called
		 * if the associated thread cannot immediately obtain access.
		 *
		 * @param	waitQueue	the queue that the associated thread is
		 *				now waiting on.
		 *
		 * @see	nachos.threads.ThreadQueue#waitForAccess
		 */
		public void waitForAccess(PriorityQueue waitQueue)
		{
			if(waitQueue.transferPriority)
			{
				waitingQueues.add(waitQueue);
				waitQueue.valid=false;
			}
			waitTime=Machine.timer().getTime();
			//System.out.println(waitTime);
			// implement me
		}

		/**
		 * Called when the associated thread has acquired access to whatever is
		 * guarded by <tt>waitQueue</tt>. This can occur either as a result of
		 * <tt>acquire(thread)</tt> being invoked on <tt>waitQueue</tt> (where
		 * <tt>thread</tt> is the associated thread), or as a result of
		 * <tt>nextThread()</tt> being invoked on <tt>waitQueue</tt>.
		 *
		 * @see	nachos.threads.ThreadQueue#acquire
		 * @see	nachos.threads.ThreadQueue#nextThread
		 */
		public void acquire(PriorityQueue waitQueue)
		{
			if(waitQueue.transferPriority)
			{
				acquiredQueues.add(waitQueue);
				this.valid=false;
			}
			// implement me
		}	

		/** The thread with which this object is associated. */	   
		protected KThread thread;
		/** The priority of the associated thread. */
		protected int priority;
		protected boolean valid;
		protected int effectivePriority;
		protected HashSet<PriorityQueue> acquiredQueues;
		protected HashSet<PriorityQueue> waitingQueues;
		protected long waitTime;
    }
}

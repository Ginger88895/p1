package nachos.threads;
import nachos.ag.BoatGrader;

public class Boat
{
    static BoatGrader bg;
    
    public static void selfTest()
    {
		BoatGrader b = new BoatGrader();
		
		System.out.println("\n ***Testing Boats with only 2 children***");
		begin(100, 4, b);

		/*
		System.out.println("\n ***Testing Boats with 2 children, 1 adult***");
		begin(1, 2, b);
		*/

		/*
		System.out.println("\n ***Testing Boats with 3 children, 3 adults***");
		begin(3, 3, b);
		*/
    }

    public static void begin( int adults, int children, BoatGrader b )
    {
		// Store the externally generated autograder in a class
		// variable to be accessible by children.
		bg=b;

		// Instantiate global variables here
		
		// Create threads here. See section 3.4 of the Nachos for Java
		// Walkthrough linked from the projects page.
		//
		
		num_adults=adults;
		num_children=children;

		KThread t_adults[]=new KThread[adults];
		KThread t_children[]=new KThread[children];
		for(int i=0;i<adults;i++)
		{
			Runnable r = new Runnable()
			{
				public void run()
				{
					AdultItinerary();
				}
			};
			t_adults[i]=new KThread(r);
			t_adults[i].fork();
		}
		for(int i=0;i<children;i++)
		{
			Runnable r = new Runnable()
			{
				public void run()
				{
					ChildItinerary();
				}
			};
			t_children[i]=new KThread(r);
			t_children[i].fork();
		}
		while(true)
		{
			mainLock.acquire();
			mainCond.sleep();
			mainLock.release();

			numLock.acquire();
			if(num_adults==passed_adults&&num_children==passed_children)
				return;
			numLock.release();
		}
    }

    static void AdultItinerary()
    {
		bg.initializeAdult(); //Required for autograder interface. Must be the first thing called.
		//DO NOT PUT ANYTHING ABOVE THIS LINE. 

		/* This is where you should put your solutions. Make calls
		   to the BoatGrader to show that it is synchronized. For
		   example:
			   bg.AdultRowToMolokai();
		   indicates that an adult has rowed the boat across to Molokai
		*/
		int position=0;
		while(true)
		{
			if(position==0)
			{
				adult0.P();
				ship0.P();

				bg.AdultRowToMolokai();

				numLock.acquire();
				passed_adults+=1;
				numLock.release();

				mainLock.acquire();
				mainCond.wake();
				mainLock.release();

				ship1.V();
				adult0.V();
				position=1;

				condLock1.acquire();
				cond1.wake();
				condLock1.release();
			}
			else
			{
				adult1.P();
				while(true)
				{
					ship1.P();
					numLock.acquire();
					int x=passed_children;
					numLock.release();
					if(x==0)
						break;
					else
					{
						ship1.V();
						condLock1.acquire();
						cond1.sleep();
						condLock1.release();
					}
				}
				bg.AdultRowToOahu();
				numLock.acquire();
				passed_adults-=1;
				numLock.release();
				ship0.V();
				adult1.V();
				position=0;
			}
		}
    }

    static void ChildItinerary()
    {
		bg.initializeChild(); //Required for autograder interface. Must be the first thing called.
		//DO NOT PUT ANYTHING ABOVE THIS LINE. 
		int position=0;
		while(true)
		{
			if(position==0)
			{
				int type=0;
				child0.P();
				mutex1.acquire();
				type=waiting_children;
				waiting_children+=1;
				mutex1.release();

				if(type==0)
				{
					diff.speak(0);
					bg.ChildRowToMolokai();
				}
				else
				{
					ship0.P();
					diff.listen();
					bg.ChildRideToMolokai();

					mutex1.acquire();
					waiting_children=0;
					mutex1.release();

					numLock.acquire();
					passed_children+=2;
					numLock.release();

					mainLock.acquire();
					mainCond.wake();
					mainLock.release();

					child0.V();
					child0.V();
					ship1.V();

				}
				position=1;
			}
			else
			{
				child1.P();
				ship1.P();

				bg.ChildRowToOahu();

				numLock.acquire();
				passed_children-=1;
				numLock.release();

				ship0.V();
				child1.V();
				position=0;
			}
		}
    }

    static void SampleItinerary()
    {
		// Please note that this isn't a valid solution (you can't fit
		// all of them on the boat). Please also note that you may not
		// have a single thread calculate a solution and then just play
		// it back at the autograder -- you will be caught.
		System.out.println("\n ***Everyone piles on the boat and goes to Molokai***");
		bg.AdultRowToMolokai();
		bg.ChildRideToMolokai();
		bg.AdultRideToMolokai();
		bg.ChildRideToMolokai();
    }

	static int boatPosition=0;

	static int num_adults=0;
	static int num_children=0;

	static int passed_adults=0;
	static int passed_children=0;

	static Lock mutex1=new Lock();
	static int waiting_children=0;

	static Lock numLock=new Lock();
	static Lock boatLock=new Lock();

	static Lock mainLock=new Lock();
	static Condition mainCond=new Condition(mainLock);

	static Lock condLock0=new Lock();
	static Condition cond0=new Condition(condLock0);
	static Lock condLock1=new Lock();
	static Condition cond1=new Condition(condLock1);

	static Semaphore child0=new Semaphore(2);
	static Semaphore adult0=new Semaphore(1);
	static Semaphore ship0=new Semaphore(1);

	static Semaphore child1=new Semaphore(1);
	static Semaphore adult1=new Semaphore(1);
	static Semaphore ship1=new Semaphore(0);

	static Communicator diff=new Communicator();
}


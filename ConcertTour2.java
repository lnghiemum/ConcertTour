// Code for "A Heuristic Method for Scheduling Band Concert Tours"
// 

import java.util.*;
import java.io.*;

public class ConcertTour2 {
	private static final String AvailabilityFile = "C:\\Users\\Linh Nghiem\\Documents"
			+ "\\Concert Schedule Optimization\\Data for programming\\AvailabilityData.csv";
	private static final String DaysFile = "C:\\Users\\Linh Nghiem\\Documents"
			+ "\\Concert Schedule Optimization\\Data for programming\\Days.csv";
	private static final String MileageFile = "C:\\Users\\Linh Nghiem\\Documents"
			+ "\\Concert Schedule Optimization\\Data for programming\\Mileage.csv";
	private static final int breakDay = 4;
	// Weight for cost functions, can change this one for appropriate condition
	private static final double MAX_TIME = 1800;
	private static final double WEIGHT_GOOD_DAYS = -200;
	private static final double WEIGHT_BAD_DAYS = 200;
	private static final double WEIGHT_TRAVEL = 10;
	private static final double PENALTY_AVAIL_TYPE_1 = 10000;
	private static final double PENALTY_AVAIL_TYPE_2 = 1000000;
	private static final double PENALTY_BREAK = 10000;
	private static final double	PENALTY_TRAVEL_1_DAY = 10000;
	private static final double	PENALTY_TRAVEL_1_DAY_MORE = 2000000;
	// Parameters of Simulated Annealing
	private static final int INITIAL_TEMPERATURE = 2500;
	private static final int TEMP_LIMIT = 500;
	private static final int ITER = 2500;
	private static final double ALPHA = 0.80;// fixed
	
	public static void main(String[] args)throws IOException  {
		
		PrintWriter out = new PrintWriter(new FileWriter("C:\\Users\\Linh Nghiem\\Documents\\FinalResult9.txt"));
				
		String [][] AvailabilityData = ReadAvailabilityCSV(AvailabilityFile);
		double [][] Availability = Availability(AvailabilityData);
		Availability [12][3] = 1; //New Orleans // 
		Availability [20][6] = 1; // Los-Angeles
		Availability [21][7] = 1;
		
		//System.out.println(Availability.length);
		double [][] Days = ReadCSVGeneral(DaysFile);
		//System.out.println(Days.length);
		double [][] Mileage = ReadCSVGeneral(MileageFile);
		
		String [] Date = getDays(getDate(AvailabilityFile));
		String [] CityName = getCityName(AvailabilityFile);
		int [] OriginalTour = new int [Availability.length];
		int [] tour = new int [Availability.length];
		int [] initialTour = new int [Availability.length];
		int [] mainIncumbent = new int [Availability.length];
		double mainIncumbentCost = -1;
		
		long startTime = System.nanoTime();
		long currentTime;
		double elapsedTime = 0;
		
		out.println("ASSUMPTION: " );
		out.println(" Break: " + breakDay + " days");
		out.println(" Max time: " + MAX_TIME + " second");
		out.println();
		out.println("WEIGHTS IN COST FUNCTION: ");
		out.println(" Good Days: " + WEIGHT_GOOD_DAYS);
		out.println(" Bad Days: " + WEIGHT_BAD_DAYS);
		out.println(" Mileage: " + WEIGHT_TRAVEL );
		out.println();
		out.println("PENALTY FOR REQUIREMENT VIOLATION");
		out.println(" Availability Violation Type 1 refers to high position in the waitlist.");
		out.println(" Availability Violation Type 2 refers to the case that is impossible to get the venue.");
		out.println(" Availability Violation Type 1: " + PENALTY_AVAIL_TYPE_1);
		out.println(" Availability Violation Type 2: " + PENALTY_AVAIL_TYPE_2);
		out.println(" Break Violation: " + PENALTY_BREAK);
		out.println(" Separation Violation 1 day: " + PENALTY_TRAVEL_1_DAY);
		out.println(" Separation Violation more than 1 day: " + PENALTY_TRAVEL_1_DAY_MORE);
		out.println();
		out.println("SIMULATED ANNEALING PARAMETERS");
		out.println(" Initial temperature: " + INITIAL_TEMPERATURE );
		out.println(" Temperature limit: " + TEMP_LIMIT);
		out.println(" Iteration: " + ITER);
		out.println(" Alpha: " + ALPHA );
		out.println();
		
		while (elapsedTime < MAX_TIME) {
			
			OriginalTour = initialSolution(Availability, Days, nearestNeighbor(Days));
		 	OriginalTour = swapBackward(OriginalTour, Availability);
			initialTour = Arrays.copyOf(OriginalTour, OriginalTour.length);
				
			tour = simulatedAnnealing1(initialTour, breakDay, Days, Availability, Mileage, Date);

			if (mainIncumbentCost == -1 || mainIncumbentCost > cost2(tour, breakDay, Mileage, Availability, Days, Date)) {
				System.arraycopy(tour, 0, mainIncumbent, 0, tour.length);
				mainIncumbentCost = cost2(mainIncumbent, breakDay, Mileage, Availability, Days, Date);
			}
			out.println();
			
			
			int [] AvailVio = countAvailVio(tour, Availability);
			int [] SeparVio =countTravelVio(tour, Days);
			
			
			if (AvailVio[1] == 0 && SeparVio[1] == 0) {
				out.println("Final Tour");
				printOut (tour, Availability, Days, Mileage,out, Date);
				printDetails (tour, CityName, getDate(AvailabilityFile), out);
				if (AvailVio[0] !=0) {
					printAvailabilityViolation (tour, Availability, AvailabilityData, 
						CityName, getDate(AvailabilityFile), out);
				}	
				if (SeparVio[0] != 0) {
					printSeparationVio (tour, Days, out, CityName);
				}
			}	
			currentTime = System.nanoTime();
			elapsedTime = (currentTime - startTime)/ Math.pow(10, 9);
		}
		out.println();
		int [] AvailVio = countAvailVio(mainIncumbent, Availability);
		int [] SeparVio =countTravelVio(mainIncumbent, Days);
		
		if (AvailVio[1] == 0 && SeparVio[1] == 0) {
			out.println("Best Tour:");
			printOut (mainIncumbent, Availability, Days, Mileage,out, Date);
			printDetails (mainIncumbent, CityName, getDate(AvailabilityFile), out);
			if (AvailVio[0] !=0) {
				printAvailabilityViolation (mainIncumbent, Availability, AvailabilityData, 
					CityName, getDate(AvailabilityFile), out);
			}	
			if (SeparVio[0] != 0) {
				printSeparationVio (mainIncumbent, Days, out, CityName);
			}
		}
		out.close();
	}  
	
	
	public static double[][] ReadCSVGeneral(String fileLocation){ 
		
		//int[][] myArray = new int[][];
		
		// Get number of rows and columns
		
		Scanner  scanIn = null;
		int Rowc = 0;
		int Row = 0;
		int Col = 0;
		String InputLine = "";
				
		try
		{
			// setup a scanner
			scanIn = new Scanner (new BufferedReader (new FileReader(fileLocation)));
			
			while (scanIn.hasNextLine()) 
			{
			// Read line in from file;
				InputLine = scanIn.nextLine();
				// split the line into an Array at the commas
				String[] InArray = InputLine.split(",");
				Row++; 
				Col = InArray.length;
			}
		}
		
		catch (Exception e) {
			System.out.println(e.getMessage());
		}
		//System.out.println(Row + " "+ Col);
		
		double[][] myArray = new double [Row][Col];
		
		try
		{
		
		scanIn = new Scanner (new BufferedReader (new FileReader (fileLocation)));
			while (scanIn.hasNextLine()) 
			{
			// Read line in from file;
				InputLine = scanIn.nextLine();
				// split the line into an Array at the commas
				String[] InArray = InputLine.split(",");
				for (int index = 0; index < InArray.length; index ++) {
					myArray[Rowc][index] = Double.parseDouble(InArray[index]);
					
				}
				// increment the row in myArray
				Rowc++;
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		return myArray;
	}
	
	
	public static String[][] ReadAvailabilityCSV (String fileLocation) {
	
	// Get number of rows and columns
	
		Scanner  scanIn = null;
		int Rowc = 0;
		int Row = 0;
		int Col = 0;
		String InputLine = "";
			
		try {
		// setup a scanner
			scanIn = new Scanner (new BufferedReader (new FileReader(fileLocation)));
		
			while (scanIn.hasNextLine()) {
				// Read line in from file;
				InputLine = scanIn.nextLine();
			// split the line into an Array at the commas
				InputLine = InputLine.replaceAll(",,", ",***,");
				InputLine = InputLine.replaceAll(",,", ",***,");
			//System.out.println(InputLine);
				StringTokenizer InArray = new StringTokenizer(InputLine, ",");
				Row++; 
				Col = InArray.countTokens();
			}
		}
	
		catch (Exception e) {
			System.out.println(e.getMessage());
		}
		
		System.out.println(Row + " "+ Col);
	
		String[][] AvailabilityData = new String [Row][Col];
	
		try {
	
			scanIn = new Scanner (new BufferedReader (new FileReader (fileLocation)));
		
			while (scanIn.hasNextLine()) {
			// Read line in from file;
			
				InputLine = scanIn.nextLine();
				InputLine = InputLine.replaceAll(",,", ",blank,");
				InputLine = InputLine.replaceAll(",,", ",blank,");
				String[] InArray = InputLine.split(",");
				for (int index = 0; index < InArray.length; index ++) {
				//System.out.println(InArray[index]);	
					AvailabilityData[Rowc][index] = InArray[index];
				}	
					
					
			// increment the row in myArray
			Rowc++;				
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		return AvailabilityData;
	}
		
	public static double [][] Availability(String[][] AvailabilityData) {
		
		int Row = AvailabilityData.length;
		int Col = AvailabilityData[0].length;
		double myArray[][] = new double [Row] [Col];
		
		for (int i = 0; i < Row; i++){
			for (int j = 0; j< Col; j++) {
				if (AvailabilityData[i][j] == null || AvailabilityData[i][j].equals("c")|| AvailabilityData[i][j].equals("blank") ) {
					myArray[i][j] = -1;
				}
				else if (AvailabilityData[i][j].equals("o")|| AvailabilityData[i][j].equals("1h") ||
						AvailabilityData[i][j].equals("2h")|| AvailabilityData[i][j].equals("3h")|| AvailabilityData[i][j].equals("o/h")){
				    myArray[i][j] = 1;
				} else { 
				    myArray[i][j] = 0; 
				}
	
			}
		}
		double[][] Availability = new double[Row-1][Col-2];
		for (int i = 0; i < Row-1; i++) {
			for (int j = 0; j< Col-2; j++) {
				Availability[i][j] = myArray[i+1][j+2];
			}
		}
	return Availability;	

	}
	
	public static String[] getDate (String fileLocation) {
	
		Scanner  scanIn = null;
		int Row = 0;
		String InputLine = "";
		ArrayList <String> Date = new ArrayList<String>();
		
		try {
		// setup a scanner
		scanIn = new Scanner (new BufferedReader (new FileReader(fileLocation)));
		
			while (scanIn.hasNextLine()) {
				// Read line in from file;
				InputLine = scanIn.nextLine();
				// split the line into an Array at the commas
				InputLine = InputLine.replaceAll(",,", ",***,");
				InputLine = InputLine.replaceAll(",,", ",***,");
				String[] InArray = InputLine.split(",");
				Date.add(InArray[0]);
				Row++; 
			}
		}catch (Exception e) {
			System.out.println(e.getMessage());
		}
		Date.remove(0);
		String [] DateString = new String[Date.size()];
		DateString = Date.toArray(DateString);
		
		return DateString;
	}
	
	public static String[] getCityName (String fileLocation) {
		
		Scanner  scanIn = null;
		String InputLine = "";
		ArrayList <String> CityName = new ArrayList<String>();
		
		try {
		// setup a scanner
		scanIn = new Scanner (new BufferedReader (new FileReader(fileLocation)));
		
				// Read line in from file;
				InputLine = scanIn.nextLine();
				// split the line into an Array at the commas
				InputLine = InputLine.replaceAll(",,", ",***,");
				InputLine = InputLine.replaceAll(",,", ",***,");
				String[] InArray = InputLine.split(",");
				for (int index = 0; index < InArray.length; index++) {
					CityName.add(InArray[index]);	
				}
				

		}catch (Exception e) {
			System.out.println(e.getMessage());
		}
		CityName.remove(1);
		String [] City = new String[CityName.size()];
		City= CityName.toArray(City);

		return City;
	}	
	
	
	public static double product(int [] tour, int i, int X) {
		
		double product = 1;
		for (int index = i; index <= i + X; index ++) {
			product = product* tour[index];
		}
		return product;
	}
	
			
	
	private static int[] nearestNeighbor (double [][] Days) {
		
		// Finding a list of city
		
		int [] list = new int [Days.length];
		double min;
		int control = 0 ; // current point in the list
		int nextCity = 0;
		list[control]= (int) (Math.random() * Days.length) + 1;
		
		//System.out.println("The starting city is " + tour[0]);
		int countVisited = 1;
		// Finding the nearest neighbourhood
		while (countVisited < Days.length) { // While the number of cities visited less than total number of cities
		
			// Finding the nearest distance from the current city
			
			min = 1000000;
			for (int index = 0; index < Days.length; index++) {
				if (Days[list[control]-1][index] != 0 && Days[list[control]-1][index] < min 
						&& !isVisited(index+1, list)) { // City ID = index + 1;
					min = Days[list[control]-1][index];
					nextCity = index+1;
				}	
			}
			list[control+1] = nextCity;
			  
			
			control++;
			countVisited++;
		}
		
			boolean changed;
			do {
				changed = false;
				for (int i = 0; i < list.length-1; i++) {
					for (int j = i+1; j< list.length; j++) {
						changed = swapNN(i,j,list,Days);
						if (changed == true) {
							int aux = list[i];
							list[i] = list[j];
							list[j] = aux;
						}
					}
				}
			} while (changed == true);
			//System.out.println(Arrays.toString(list));
			///System.out.println(totalDayDistance(list,Days));
			
			
		return list;
	}
	
	public static int[] initialSolution(double [][] Availability, double[][] Days, int [] list) {
		
		int [] tour = new int [Availability.length];
		int current = 0;
		tour[0] = list[0];
		
		for (int index = 1; index < list.length; index++) {
			current += Days[list[index]-1][list[index-1]-1];
			tour[current] = list[index]; 
		}
		//System.out.println(Arrays.toString(tour));
		return tour;
		
		
		
	}
	
	// Check if a city is visited in the tour

	public static boolean isVisited(int CityID, int[] tour) {
		int index;
		
		for (index = 0; index <tour.length; index ++) {
			if (tour[index] == CityID) return true;
		}
		return false;		
	}
	
	// Count total cities visited in the tour
	
	public static int countCity(int [] tour) {
		
		int count = 0;
		
		for (int index = 0; index < tour.length; index ++) {
			if (tour[index] != 0) count++;
		}
		return count;
		
	}
	public static int totalDayDistance (int[] tour, double[][] Days) {
		
		int distance = 0;
		for (int index = 0; index < tour.length-1; index++) {
			distance += Days[tour[index]-1][tour[index+1]-1]; 
		}
		return distance;
		
	}

	
	public static double totalMileageDistance (int[] tour, double[][] Mileage) {
		
		double distance = 0;
		int next = 0;
		for (int i = 0; i < tour.length-1; i++) {
			if (tour[i]!= 0) {
				for (int j = i+1; j < tour.length; j++) {
					if (tour[j] != 0) {
						next = j;
						break; 
					}
				}
			distance += Mileage[tour[i]-1][tour[next]-1];	
			}	
			 
		}
		//System.out.println(distance);
		return distance;
		
	}
	
	public static boolean swapNN (int i, int j, int[] list, double[][] Days) {
		
		int[] newList = Arrays.copyOf(list, list.length);
		
		int aux = newList[i];
		newList[i] = newList[j];
		newList[j] = aux;
		
		if (totalDayDistance(newList,Days) < totalDayDistance(list, Days)) {
			return true;
		}
		return false;
		
	}
	// Swap unavailable days with zeros at the end of the tour
	public static int[] swapBackward(int[] tour, double[][]Availability) {
		
		int lastCity= tour.length-1;
		for (int index = tour.length-1; index >= 0; index--) {
			if (tour[index]!=0) {
				lastCity = index+1;
				break;
			}
		}
		
		for (int index = 0; index < lastCity; index++) {
			if (tour[index]!=0 && Availability[index][tour[index]-1]== 0) { 
				//System.out.println(index + " " + tour[index]);
				for (int index2 = tour.length-1; index2 >= lastCity; index2--) {
					if (tour[index2] == 0 && Availability[index2][tour[index]-1] == 1) {
						tour[index2] = tour[index];
						tour[index] = 0;
						break;
					}
				}
			}	
		}
		return tour;
	}
	public static int countGoodDays(int []tour, String[] Date) {
	
		int goodDays = 0;
			
		for (int index = 0; index< tour.length; index++) {
			// Friday and Saturday are good 
			if (tour[index]!= 0 && ((Date[index].equals("Fri")) || Date[index].equals("Sat"))) 
				goodDays++;
				
			}
		
		return goodDays;
			
	}
	
	public static int countBadDays(int []tour, String[] Date) {
	
		int badDays = 0;
			
		// Monday and Tuesday are bad
		for (int index = 0; index< tour.length; index++) {
			if (tour[index]!= 0 && ((Date[index].equals("Mon")) || Date[index].equals("Tue")))  
				badDays++;
		}
		return badDays;
		
	}


	public static String[] getDays (String[] Date) {
			
			String [] DaysWeek = new String[Date.length];
			
			for (int index = 0; index < DaysWeek.length; index++) {
				DaysWeek[index] = Date[index].substring(0,3);
			}
			
			return DaysWeek;
			
	}
		
	public static int countTotal(int[] tour) {
	
		int count = 0;
			
		for (int index = 0; index < tour.length; index++) {
			if (tour[index]!=0) count++;
		}
		return count;
	}
		
		// Simulated Annealing 2: No feasibility check
		
			
	public static int[] simulatedAnnealing1(int[] tour, int X, double[][] Days, double[][] Availability, 
												double[][] Mileage, String [] Date) {
			// X = break requirement
		int [] newTour = new int [Availability.length];
		double costNewTour;
		double costTour;
		int [] incumbent = new int [tour.length];
		double incumbentCost = cost2(tour, X, Mileage, Availability, Days, Date);
		double t = INITIAL_TEMPERATURE;
		int iteration;
		double x;
		double d; // difference
			
			
		t = INITIAL_TEMPERATURE;
	
		while (t > TEMP_LIMIT) {
				
			for (iteration = 0; iteration < ITER; iteration ++) {
			//for (iteration = 0; iteration < 3; iteration ++) {
			//System.out.println(iteration);
				System.arraycopy(tour,0,newTour,0,tour.length);
				newTour = swap2(newTour, Days);
				costNewTour = cost2(newTour, X, Mileage, Availability,Days, Date);
				costTour = cost2(tour, X, Mileage, Availability, Days, Date);
					
					/*System.out.print("Old tour:\n  ");
					System.out.println(Arrays.toString(tour));
					System.out.println("  " + "Cost = " + costTour);
					System.out.print("New tour:\n  ");
					System.out.println(Arrays.toString(newTour));
					System.out.println("  " + "Cost = " + costNewTour);
					*/
					d = costNewTour - costTour;
						if (d < 0) {
							//System.out.println("d is < 0:");
							System.arraycopy(newTour,0,tour,0,newTour.length);
							if (costNewTour < incumbentCost) {
								//System.out.println("  Updated incumbent.");
								System.arraycopy(newTour,0,incumbent,0,newTour.length);
								incumbentCost = costNewTour;
							}
						} else {
							//System.out.println("d is >= 0:");
							x = Math.random();
							if (x < Math.exp(-d/t)) {
								//System.out.println("  Accepted bad move.");
								System.arraycopy(tour,0,newTour,0,tour.length);
							}
							/*else {
								System.out.println("  Didn't accept bad move.");
							}*/
						} 				
						//System.out.println();
						//System.out.flush();
				}
				t = t * ALPHA;	
				//t = TEMP_LIMIT;
			}
			
			// Prints the overall best
			//System.out.println(incumbentCost);
		return incumbent;
	}
	
	public static double cost2 (int[] tour, int X, double[][] Mileage, double [][] Availability, double[][] Days, String[] Date) {
			
		double cost;
		int [] AvailVio = countAvailVio(tour, Availability);
		int [] SeparationVio = countTravelVio(tour, Days);
		
		cost = WEIGHT_BAD_DAYS * countBadDays(tour,Date) + WEIGHT_GOOD_DAYS * countGoodDays(tour,Date)
				+ WEIGHT_TRAVEL * totalMileageDistance(tour, Mileage) + PENALTY_AVAIL_TYPE_1 * AvailVio[0]
				+ PENALTY_AVAIL_TYPE_2 * AvailVio[1]
					+ PENALTY_BREAK * countBreakVio(tour, X)+ PENALTY_TRAVEL_1_DAY * SeparationVio[0] + PENALTY_TRAVEL_1_DAY_MORE * SeparationVio[1] ;
		return cost;
		}
		
		
	private static int[] swap2 (int[] tour, double[][] Days) {
			
		int i=0, j=0; 
		do {
				
			i = (int) (Math.random()* tour.length);
			j = (int) (Math.random()* tour.length);
			
		} while (!isSwappable(tour,i,j,Days));
		
			int aux = tour[i];
			tour[i] = tour[j];
			tour[j] = aux;	
			 
			
		return tour;	
		}
		
		// Need an update to tell whether a swap is feasible 
		
	public static boolean isSwappable (int[] tour, int i, int j, double[][] Days) {
									
			if ((i == j) || (tour[i] == 0 && tour[j] == 0)) return false;
			else {
				return true;
				
			}
		}
			
	public static int countBreakVio(int tour[], int X) {
			
			int count = 0;
			for (int index = 0; index < tour.length-X; index++) {
				if (product(tour, index, X) !=0 ) count++;
				
			}
			//System.out.println(count);
			
			return count;
		}	
		
	public static int[] countAvailVio(int tour[], double[][] Availability) {
			// 2 elements: type 1 and type 2
			// count[0]: type 1; count[1]: type 2
			int[] count = new int[2];
			for (int index = 0; index < tour.length; index++) {
				if (tour[index]!=0 && Availability[index][tour[index]-1] == 0) count[0]++;
				else if ((tour[index]!=0 && Availability[index][tour[index]-1] == -1)) count[1]++;
			}
			//System.out.println(count);
			return count;
		}
	
	public static void printAvailabilityViolation (int[] tour, double[][] Availability, String[][] AvailabilityData, 
								String [] CityName, String [] Date, PrintWriter out) {
		out.println();
		out.println("Availability Violation:");
		for (int index = 0; index < tour.length; index++) {
			if (tour[index]!=0 && Availability[index][tour[index]-1] != 1) {
				out.println(Date[index] + " " + "at " + CityName[tour[index]]
						+ ", availability status: " + AvailabilityData[index+1][tour[index]+1] );
			}
		}
			
	}
	
		
	public static int[] countTravelVio (int tour[], double[][] Days) {
			
			// count[0]: type 1: 1 days
			// count [1]: type 2: > 1 days
			int count[]  =  new int[2];
			int next = 0;
			for (int i = 0; i < tour.length-1; i++) {
				if (tour[i]!= 0) {
					for (int j = i+1; j < tour.length; j++) {
						if (tour[j] != 0) {
							next = j;
							break; 
						}
					}
			if (Days[tour[i]-1][tour[next]-1] - (next-i) == 1) count[0]++;
			else if (Days[tour[i]-1][tour[next]-1] - (next-i) > 1)count[1]++;
				}	
			 
			}
			return count;
		}	
	
	
	public static void printSeparationVio (int tour[], double[][] Days, PrintWriter out, String [] CityName) {
			
			out.println();
			out.println("Separation Violation:");
			int next = 0;
			for (int i = 0; i < tour.length-1; i++) {
				if (tour[i]!= 0) {
					for (int j = i+1; j < tour.length; j++) {
						if (tour[j] != 0) {
							next = j;
							break; 
						}
					}
					if (next-i < Days[tour[i]-1][tour[next]-1]){
						out.println("It normally takes " + Days[tour[i]-1][tour[next]-1] + " days to travel from " + CityName[tour[i]] +
							 "(city " + tour[i] + ") to " + CityName[tour[next]] + "(city " + tour[next] + ")");
						out.println("Now it takes " + (next-i) + " days");	
					}	
				} 	
			}	
			 
	}
		
	public static void printOut(int [] tour, double[][] Availability, double [][] Days, 
									double[][] Mileage, PrintWriter out, String[] Date) throws IOException {
		
			int[] AvailVio = countAvailVio(tour, Availability);
			int [] SeparationVio = countTravelVio(tour, Days);
			out.println(Arrays.toString(tour));
			out.println("Good days: " + countGoodDays(tour,Date));
			out.println("Bad Days: " +  countBadDays(tour, Date));  // checked
			out.println("Number of cities in the tour: " + countTotal(tour));
			out.println("Total mileage: " + (int)(totalMileageDistance(tour,Mileage))); // checked
			out.println("Availability violation Type 1: " + AvailVio[0]);
			out.println("Availability violation Type 2: " + AvailVio[1]);
			out.println("Break violation: " + countBreakVio(tour, breakDay));    
			out.println("Separation violation 1 day: " + SeparationVio[0]);
			out.println("Separation violation more than 1 day: " + SeparationVio[1]);
	
	}
	
	public static void printDetails (int[] tour, String[] CityName, String[] Date, PrintWriter out){
		
		out.println();
		out.println("Schedule");
		for (int index = 0; index< tour.length; index++) {
			if (tour[index]!=0) {
				out.println(Date[index]+ ", " + CityName[tour[index]]);
			}
		}			
	}
	
		
	
}
		
				
		

							
					

		


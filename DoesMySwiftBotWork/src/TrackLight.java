import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import java.util.List;
import java.util.Scanner;

import javax.imageio.ImageIO;

import swiftbot.SwiftBotAPI;
import swiftbot.SwiftBotAPI.Underlight;
import swiftbot.SwiftBotAPI.ImageSize;

import com.hopding.jrpicam.exceptions.FailedToRunRaspistillException;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import com.pi4j.util.Console;

public class TrackLight {
	
	 private static double[] highestIntensities = new double[3];
	 private static int numLightDetections = 0;
	 private static ArrayList<String> movements = new ArrayList<String>();
	 private static double totalDistance = 0.0;
	 private static long startTime = 0L;
	 private static final int MAX_LIGHT_INTENSITY = 100;
  

	public static int[] getImageIntensities(SwiftBotAPI swiftBot, String directory, String filename) throws IOException, FailedToRunRaspistillException, InterruptedException {
	    int[] intensities = new int[3];
	    try {
	    	swiftBot.takeStill("/home/pi/Documents", "light.png", ImageSize.SQUARE_480x480);;
	        String imagePath = directory + "/" + filename;
	        BufferedImage image = ImageIO.read(new File(imagePath));
	        int width = image.getWidth();
	        int height = image.getHeight();
	        int leftSum = 0;
	        int centerSum = 0;
	        int rightSum = 0;
	        for (int y = 0; y < height; y++) {
	            for (int x = 0; x < width; x++) {
	                int rgb = image.getRGB(x, y);
	                int red = (rgb >> 16) & 0xFF;
	                int green = (rgb >> 8) & 0xFF;
	                int blue = (rgb & 0xFF);
	                int intensity = (red + green + blue) / 3;
	                if (x < width / 3) {
	                    leftSum += intensity;
	                } else if (x < 2 * width / 3) {
	                    centerSum += intensity;
	                } else {
	                    rightSum += intensity;
	                }
	            }
	        }
	        intensities[0] = leftSum / (width * height / 3);
	        intensities[1] = centerSum / (width * height / 3);
	        intensities[2] = rightSum / (width * height / 3);
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	    return intensities;
	}

    
    
    
    
        public static int getHighestIntensityDirection(int left, int center, int right) {
            if (left > center && left > right) {
                return -1; // highest intensity in the left direction
            } else if (right > center && right > left) {
                return 1; // highest intensity in the right direction
            } else {
                return 0; // highest intensity in the center direction
            }
        }
        
       

    
    static SwiftBotAPI swiftBot;
    
    public static void main(String[] args) {
    	 swiftBot = new SwiftBotAPI();

    	// Start a new thread to listen for the X button press
    	 	System.out.println("Welcome to the SwiftBot Light Tracker!");
    	    System.out.println("Please choose an option:");
    	    System.out.println("1. Track light");
    	    System.out.println("2. Exit");
    	    Scanner scanner = new Scanner(System.in);
    	    int choice = scanner.nextInt();

    	    if (choice == 1) {
    	        // Call the trackLight method
    	    	System.out.println("Swiftbot will begin Tacking Light");
    	    	trackLight();
    	    } else if (choice == 2) {
    	    	System.out.println("Good By have a Nice Day");
    	        System.exit(0);
    	    } else {
    	        System.out.println("Invalid choice. Please choose again.");
    	    }
    	    scanner.close();
}

      
	        
    public static void trackLight() {
        
        boolean continueSearching = true;
        int numDetections = 0;
        int leftHighest = 0;
        int centerHighest = 0;
        int rightHighest = 0;
        long lastDetectionTime = System.currentTimeMillis();
        new Thread(() -> {
	        System.out.println("Press the 'X' button on the Swiftbot to end the program.");
	        swiftBot.BUTTON_X.addListener(new GpioPinListenerDigital() {
	            public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
	                if (event.getState().isLow()) {
	                    displayLog();
	                }
	            }
	        });
	    }).start();
        try {
            while (continueSearching) {
                

                int[] intensities = getImageIntensities(swiftBot, "/home/pi/Documents", "light.png");
                int leftIntensity = intensities[0];
                int centerIntensity = intensities[1];
                int rightIntensity = intensities[2];

                if (leftIntensity > leftHighest) {
                    leftHighest = leftIntensity;
                }

                if (centerIntensity > centerHighest) {
                    centerHighest = centerIntensity;
                }

                if (rightIntensity > rightHighest) {
                    rightHighest = rightIntensity;
                }

                int direction = getHighestIntensityDirection(leftIntensity, centerIntensity, rightIntensity);

                swiftBot.fillUnderlights(255, 0, 0);
                Thread.sleep(1000);

                if (direction == 0) {
                    
                    swiftBot.move(90, 90, 500);
                } else if (direction == 1) {
                   
                    swiftBot.move(90, 0, 500);
                } else {
                    
                    swiftBot.move(0, 90, 500);
                }
             // Check for light detection
    	        if( getImageIntensities(swiftBot, "/home/pi/Documents", "light.png")[1] > 0) {
    	            numDetections++;
    	            totalDistance += 15; //  Swiftbot moves 15 cm for each detection
    	            lastDetectionTime = System.currentTimeMillis();
    	        }
    	       

    	        // Check if 5 seconds have elapsed without a detection
    	    if (System.currentTimeMillis() - lastDetectionTime > 5000) {
    	       
    	        
    	        lastDetectionTime = System.currentTimeMillis(); // Reset last detection time
    	        
    	        int randomDirection = (int) (Math.random() * 2); // Generate a random number between 0 and 1
    	        if (randomDirection == 0) {
    	            
    	            swiftBot.move(-100, 100, 500);
    	        } else {
    	            
    	            swiftBot.move(100, -100, 500);
    	        }}
    	    
    	    }
            }
    	           
    	   
    	            catch (NullPointerException e) {
        	            // Catch any NullPointerExceptions and print an error message
        	            
        	        } catch (Exception e) {
        	            // Catch any other exceptions and print the stack trace
        	            e.printStackTrace();
        	        }
        }

    public static void displayLog() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Do you want to display the log? (yes/no)");
        String choice = scanner.nextLine();
        if (choice.equalsIgnoreCase("yes")) {
            System.out.println("Highest Intensities: ");
            System.out.printf("Left: %.2f %%\n", ((double) highestIntensities[0] / MAX_LIGHT_INTENSITY) * 100);
            System.out.printf("Centre: %.2f %%\n", ((double) highestIntensities[1] / MAX_LIGHT_INTENSITY) * 100);
            System.out.printf("Right: %.2f %%\n", ((double) highestIntensities[2] / MAX_LIGHT_INTENSITY) * 100);

            System.out.println("Number of Light Detections: " + numLightDetections);

            System.out.println("Movements: ");
            for (String movement : movements) {
                System.out.println(movement);
            }

            System.out.println("Total Distance Travelled: " + totalDistance + "cm");

            long endTime = System.currentTimeMillis();
            long duration = (endTime - startTime) / 1000;
            System.out.println("Duration: " + duration + " seconds");
            System.exit(0);

        } else if (choice.equalsIgnoreCase("no")) {
        	
            System.out.println("Log display cancelled.");
            System.exit(0);
            
        } else {
            System.out.println("Invalid choice. Please type 'yes' or 'no'.");}
        scanner.close();
}
}

    
   
	
 
            
 


       
    
	       
	    
    
	        
	        

       
    
    	
        	
      	
    
    
    
    
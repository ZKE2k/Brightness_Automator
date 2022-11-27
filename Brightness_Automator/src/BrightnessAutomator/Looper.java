package BrightnessAutomator;

import java.io.File;
import javax.imageio.ImageIO;
import java.io.IOException;
import com.github.sarxos.webcam.Webcam;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.atomic.AtomicBoolean; 
import java.time.*;
        
// initializes class and implements runnable format.
public class Looper implements Runnable {

    //variable for checking if program is running
    private AtomicBoolean keepRunning;

    // loops the program
    public Looper() {
        keepRunning = new AtomicBoolean(true);
    }

    // stops the loop
    public void stop() {
        keepRunning.set(false);
    }
    
    // pauses the program for an input amount of milliseconds.
     public static void wait(int ms)
    {
        try
        {
            Thread.sleep(ms);
        }
        catch(InterruptedException ex)
        {
            Thread.currentThread().interrupt();
        }
    }
    
     // stores the max and min brightness values.
    static int maxb = 100;
    static int minb = 1;
    
    // updates the max and min brightness values.
    static void maxupd(int max) {
        maxb = max;
    }
    static void minupd(int min) {
        minb = min;
    }
    
    // initiates the brightness change using a string variable as a powershell command.
    static void change(int brightness) throws IOException {
        //Creates a powerShell command that will set the brightness to the requested value (0-100), after the requested delay (in milliseconds) has passed.
        String s = String.format("$brightness = %d;", brightness)
                + "$delay = 0;"
                + "$myMonitor = Get-WmiObject -Namespace root\\wmi -Class WmiMonitorBrightnessMethods;"
                + "$myMonitor.WmiSetBrightness($delay, $brightness)";
        String command = "powershell.exe  " + s;
        // Executing the command
        Process powerShellProcess = Runtime.getRuntime().exec(command);

        powerShellProcess.getOutputStream().close();

        //Report any error messages
        String line;

        BufferedReader stderr = new BufferedReader(new InputStreamReader(
                powerShellProcess.getErrorStream()));
        line = stderr.readLine();
        if (line != null)
        {
            System.err.println("Error: External monitors are not compatible with program.");

        }
        stderr.close();

    }
    
    // gets image brightness using an iterating for loop to count each pixel and average the brightness.
    public static void lum(String ffile) {
        BufferedImage img=null;
        
        File myFile = new File(ffile);
        
        try {
            if (myFile.isFile()) {
                img = ImageIO.read(myFile);
                int width=0;
                int height=0;
                int count=0;
                int avg=0;
                    if(img!=null){
                        width = img.getWidth();
                        height = img.getHeight(); 
                    }
                for(int x=0; x<width; x++){
                    for (int y=0;y<height;y++) {
                        int pixelCol = img.getRGB(x, y);
                        int a = (pixelCol >>> 24)  & 0xff;
                        int r = (pixelCol >>> 16)  & 0xff;
                        int g = (pixelCol >>> 8)  & 0xff;
                        int b = pixelCol & 0xff;
                        
                        double lum = (0.2125*r) + (0.7152*g) + (0.0722*b);
                        avg+= lum;
                        count++;
                    }
                }
                avg = avg/count;
                double br =(double) avg / 255 * 100;
                int brightness = (int) br;
                if (brightness < minb){
                    brightness = minb;
                }
                else if (brightness > maxb) {
                    brightness = maxb;
                }
                System.out.println("The Average brightness is:" +brightness);
                change(brightness);
            }
        }catch(Exception e){
                    System.out.println("There was a problem" + e);
                    }
    }
    // gets default webcam as variable.
    static Webcam web = Webcam.getDefault();
    
    // updates webcam to drop down list choice.
    static void webupd(Webcam webb) {
        web = webb;
    }
    
    // variable for type of run.
    static boolean status = true;
    
    // updates status when called.
    static void stateupd(boolean state) {
        status = state;
    }
    
    // alternate method to run program utilizing the time of day and the min/max brightnesses.
    static void altrun(int t) {
        int distance = maxb - minb;
        System.out.println(t);
        double iter = distance / 12;
        System.out.println(iter);
        int ival = (int) iter;
        System.out.println(ival);
       try {
        if (t > 0 & t < 12) {
            int bright = minb + ival * t;
            change(bright);
        }
        else if (t == 12) {
            change(maxb);
        }
        else if (t > 12 & t <=23) {
            int hour = t - 12;
            int bright = maxb - ival * hour;
            change(bright);
        }
        else {
            change(minb);
        }
       }
       catch(Exception e){
           System.out.println(e);
       }
    }
    
    // main run for the program, while looping, checks status of the run and runs respective processes to perform the program.
    @Override
    public void run() {
        try {
            while (keepRunning.get()) {
                if (status == true) {
                wait(1000);
                web.open();
                ImageIO.write(web.getImage(), "JPG", new File("webimg.jpg"));
                File img = new File("webimg.jpg");
                String path = img.getPath();
                lum(path);
                }
                else if (status == false) {
                    wait(1000);
                    LocalTime time = java.time.LocalTime.now();
                    int hour = time.getHour();
                    altrun(hour);
                }
            }
        }catch(Exception ex) {
            System.out.println (ex.toString());
        }
    }

}
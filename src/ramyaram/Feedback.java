package ramyaram;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.Timer;

import sun.audio.*;

public class Feedback {
  
    public static void addSound(char sign) throws IOException{
        String loc = "";
        if (sign == '+'){
            loc = "media/good.au";
        }
        else if (sign == '-') {
            loc = "media/bad.au";
        }
        String soundFile = loc;
        InputStream in = new FileInputStream(soundFile);
        AudioStream audioStream = new AudioStream(in);
        AudioPlayer.player.start(audioStream);
    }
    
    public static void addFeedback(double ScoreChange, int gameTick) throws IOException{
        String loc = "";
        SplashScreen m = new SplashScreen();
        m.showSplash(loc);
        if(ScoreChange > 0){
            addSound('+');
//            if (ScoreChange == 10){
//                loc = "C:\\Users\\mattj\\OneDrive\\Pictures\\IRG\\+10.jpg";
//            }
//            else if (ScoreChange == 5){
//                loc = "C:\\Users\\mattj\\OneDrive\\Pictures\\IRG\\+5.jpg";
//            }
//            else if (ScoreChange == 1){
//                loc = "C:\\Users\\mattj\\OneDrive\\Pictures\\IRG\\+1.jpg";
//            }
//        
//            else {
//                loc = "C:\\Users\\mattj\\OneDrive\\Pictures\\IRG\\plus.jpg";
//                }
            loc = "media/+10.jpg";
        }
        else if (ScoreChange < 0){
            addSound('-');
//            if (ScoreChange == -1){
//                loc = "C:\\Users\\mattj\\OneDrive\\Pictures\\IRG\\-1.jpg";
//            }
//            else if (ScoreChange == -5){
//                loc = "C:\\Users\\mattj\\OneDrive\\Pictures\\IRG\\-5.jpg";
//            }
//            else if (ScoreChange == -10){
//                loc = "C:\\Users\\mattj\\OneDrive\\Pictures\\IRG\\-10.jpg";
//            }
//            else {
//                loc = "C:\\Users\\mattj\\OneDrive\\Pictures\\IRG\\minus.jpg";
//           }
            loc = "media/-10.jpg";
        }
        
       SplashScreen n = new SplashScreen();
       n.showSplash(loc);
       
//            System.out.println(ScoreChange);
        
        }
    }
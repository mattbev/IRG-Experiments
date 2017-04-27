package ramyaram;

import java.awt.*;
import javax.swing.*;

public class SplashScreen extends JWindow {
  /**
     * 
     */
    private static final long serialVersionUID = 1L;
//private int duration;
  public SplashScreen() {
    //duration = d;
  }

  public void showSplash(String loc) {
    JPanel content = (JPanel)getContentPane();
    content.setBackground(Color.white);

    // Set the window's bounds, centering the window
    int width = 300;
    int height = 300;
    Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
    int x = 0; //1*(screen.width-width)/20;
    int y = 0; //1*(screen.height-height)/10;
    setBounds(x,y,width,height);

    // Build the splash screen
    final ImageIcon icon = new ImageIcon(loc);
    JLabel label = new JLabel(icon);
    content.add(label, BorderLayout.CENTER);
    Color oraRed = new Color(156, 20, 20,  255);
    content.setBorder(BorderFactory.createLineBorder(oraRed, 10));

    // Display it
    setVisible(true);

//    try {
//        Thread.sleep(duration);
//    } catch (InterruptedException e) {
//        e.printStackTrace();
//    }
//    setVisible(false);
  }
}
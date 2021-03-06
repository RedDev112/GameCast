package GameCore;

import java.awt.Cursor;
import java.awt.DisplayMode;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferStrategy;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.WindowConstants;

/**
 * AnimatedFrame allows for smooth animation
 * on pretty much all hardware. Also handles events.
 * <p>
 * I'm unsure how most of this works. It was /not/
 * created by me, Tyler. I got this class in a
 * high school Computer Science class, from my
 * teacher, Mr. Stejskal. I'm not sure if he
 * created it, or not, or anything else related
 * to it's origin.
 */
public class AnimatedFrame extends JFrame implements Runnable
{
    public enum Event
    {
	
    }
    
    // Graphics mode info
    private GraphicsDevice dev;
    private DisplayMode mode;
    private BufferStrategy bstrat;
    private Cursor hiddenCursor;
    // FPS limiter
    private int targetFPS;
    private long nanosPerUpdate;
    // FPS timing
    private long fpsTimeGoal;
    private int fpsCount;
    private int upsCount;
    private float lastFPS;
    private float lastUPS;
    // Game data
    private volatile boolean running;
    private Thread gameThread;
    private Screen currentScreen;
    //For handing events
    private Queue<Event> events;

    public AnimatedFrame()
    {
	setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
	setUndecorated(true);
	setResizable(false);
	setIgnoreRepaint(true);
	targetFPS = 60;  // How many frames are we going to generate per second?
	nanosPerUpdate = 1000000000 / targetFPS;
	events = new ConcurrentLinkedQueue<Event>();
	
	addWindowListener(new WindowAdapter()
	{
	    @Override
	    public void windowClosing(WindowEvent evt)
	    {
		formWindowClosing(evt);
	    }
	});

	addKeyListener(new KeyAdapter()
	{
	    @Override
	    public void keyPressed(KeyEvent evt)
	    {
		formKeyPressed(evt);
	    }
	});
	
	addMouseListener(new MouseAdapter()
	{
	    @Override
	    public void mouseReleased(MouseEvent evt)
	    {
		formMouseClicked(evt);
	    }
	});
	
    }

    private Screen createInitialScreen()
    {
	return null;
    }

    private void initGame()
    {
	currentScreen = createInitialScreen();

	if (initFSMode()) {

	    gameThread = new Thread(this);
	    gameThread.start();
	}else{
	    System.out.println("initFSMode() returned false; quitting.");
	    dispose();
	}
    }

    private boolean initFSMode()
    {
	GraphicsEnvironment env =
		GraphicsEnvironment.getLocalGraphicsEnvironment();
	dev = env.getDefaultScreenDevice();
	if (!dev.isFullScreenSupported()) {
	    JOptionPane.showMessageDialog(this,
		    "Full-screen mode not supported.",
		    "Error",
		    JOptionPane.WARNING_MESSAGE);
	    return false;
	}
	dev.setFullScreenWindow(this);
	mode = dev.getDisplayMode();

	// Initial frame rate guess
	lastFPS = mode.getRefreshRate();
	if (lastFPS == 0)
	    lastFPS = 30;  // Just give _some_ nonzero guess
	if (createBuffering())
	    return true;
	else
	    return false;
    }
    
    private boolean initWindowedMode()
    {
	return false;
    }

    private boolean createBuffering()
    {
	bstrat = getBufferStrategy();
	createBufferStrategy(2);
	// Wait up to 1 second for a buffer strategy to be created
	long targetNanos = System.nanoTime() + 1000000000;
	do {
	    bstrat = getBufferStrategy();
	} while (bstrat == null && targetNanos < System.nanoTime());
	if (bstrat == null) {
	    return false;
	}
	return true;
    }

    @Override
    public void run()
    {
	running = true;
	long nextFrame = System.nanoTime() + nanosPerUpdate;
	while (running)
        {
	    while (nextFrame < System.nanoTime())
            {
		updateGame();
		upsCount++;
		nextFrame += nanosPerUpdate;
	    }
            
	    drawFrame();
            
	    if (fpsTimeGoal == 0)
            {
		fpsTimeGoal = System.nanoTime() + 5000000000L;
	    } else if (fpsTimeGoal < System.nanoTime()) {
		lastFPS = fpsCount / 5.0f;
		lastUPS = upsCount / 5.0f;
		fpsTimeGoal += 5000000000L;
		System.out.println("5-second measurement: "
			+ lastFPS + " fps; " + lastUPS + " ups");
		fpsCount = 0;
		upsCount = 0;
	    } else {
		fpsCount++;
	    }
	}
    }

    public void updateGame()
    {
	currentScreen.update();
	Screen next = currentScreen.getNextScreen();
	if (next != null)
	    currentScreen = next;
    }

    private void drawFrame()
    {
	try
        {
	    Graphics2D gr = (Graphics2D)bstrat.getDrawGraphics();
	    render(gr);
	    gr.dispose();
            
	    if (!bstrat.contentsLost())
		bstrat.show();
	    else
		System.out.println("Contents Lost");
            
	    // Sync the display on some systems.
	    // (on Linux, this fixes event queue problems)
	    Toolkit.getDefaultToolkit().sync();
	} catch (Exception ex) {
	    ex.printStackTrace();
	    running = false;
	}
    }

    public void render(Graphics2D gr)
    {
	currentScreen.render(gr);
    }

    private void formWindowClosing(WindowEvent evt)
    {
	running = false;
	dev.setFullScreenWindow(null);
    }
    
    private void formKeyPressed(KeyEvent evt)
    {
	
    }

    private void formMouseClicked(MouseEvent evt)
    {
//	// If the game is playing.
//	if (currentScreen instanceof TankScreen)
//	{
//	    // Give the current tank the info to shoot.
//	    ((TankScreen)currentScreen).getCurrentTank().setTarget(evt.getX(), evt.getY());
//	    events.offer(Event.SHOOT);
//	}
//	// Can expand to allow for menu screen, and other clicks.
    }
    
    
    public static void main(String[] args)
    {
	AnimatedFrame frame = new AnimatedFrame();
	frame.initGame();
    }

    public Event getNextEvent()
    {
	return events.poll();
    }
    
    public Queue<Event> getQueue()
    {
	return events;
    }
}

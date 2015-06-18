package gameoflife;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import javax.swing.JFrame;

public class GameOfLife {

    public static int NUMX = 320;
    public static int NUMY = 320;
    
    public static JFrame frame;
    public static byte[][] cells;
    public static ArrayList<Point> active;
    public static BufferedImage db;
    public static BufferedImage db2;
    static boolean awake = true;
    static boolean rdy2draw = false;
    static Point lastp;
    
    public static void main(String[] args) {
        frame = new JFrame(){
            public void paint(Graphics g){
                DrawFrame(g);
            }
        };
        frame.setSize(640+6,640+28);
        frame.setTitle("Game Of Life");
        frame.setResizable(false);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
        
        new UtilFrame();
        
        frame.addMouseListener(new MouseListener() {

            @Override
            public void mouseClicked(MouseEvent e) {
                
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if(e.getButton() == MouseEvent.BUTTON3){
                    awake = !awake;
                    lastp = null;
                }
                else
                    lastp = new Point((int)((e.getX()-3)*(NUMX/640.0)),(int)((e.getY()-25)*(NUMY/640.0)));
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if(lastp != null && !awake){
                    if(cells[lastp.x][lastp.y]==0){
                        cells[lastp.x][lastp.y] = 1;
                        active.add(new Point(lastp.x,lastp.y));
                    }
                }
                    
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                
            }

            @Override
            public void mouseExited(MouseEvent e) {
                
            }
        });
        
        frame.addMouseMotionListener(new MouseMotionListener() {

            @Override
            public void mouseDragged(MouseEvent e) {
                if(!rdy2draw || lastp == null || awake)
                    return;
                int x = (int)((e.getX()-3)*(NUMX/640.0));
                int y = (int)((e.getY()-25)*(NUMY/640.0));
                if(x < 0 || y < 0 || x >= cells.length || y >= cells[0].length)
                    return;
                
                if(cells[x][y]==0){
                    cells[x][y] = 1;
                    active.add(new Point(x,y));
                }
                
                //FILL IN LINE
                double xdf = (x - lastp.x)/20.0;
                double ydf = (y - lastp.y)/20.0;
                for(int i = 0; i < 20; i++){
                    int xt = x - (int)(xdf*i);
                    int yt = y - (int)(ydf*i);
                    if(cells[xt][yt]==0){
                        cells[xt][yt] = 1;
                        active.add(new Point(xt,yt));
                    }
                }
                lastp = new Point(x,y);
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                
            }
        });
        
        db = new BufferedImage(NUMX, NUMY, BufferedImage.TYPE_INT_RGB);
        db2 = new BufferedImage(640, 640, BufferedImage.TYPE_INT_RGB);
        active = new ArrayList<Point>();
        cells = new byte[db.getWidth()][db.getHeight()];
        
        for(int i = 0; i < cells.length; i++)
            for(int j = 0; j < cells[0].length; j++)
                if(Math.random()<0.05){
                    cells[i][j] = 1;
                    active.add(new Point(i,j));
                }
                
        new Thread(){
            public void run(){
                while(true){
                    frame.repaint();
                }
            }
        }.start();
        
        new Thread(){
            public void run(){
                while(true){
                    while(!awake)
                        try {sleep(1);}catch(Exception e){}
                    rdy2draw = false;
                    Think();
                    rdy2draw = true;
                    //try{sleep(500);}catch(Throwable t){}
                }
            }
        }.start();
    }
    
    public static void DrawFrame(Graphics gf){
        if(db == null || db2 == null)
            return;
        //DRAW HERE
        Graphics2D g = (Graphics2D)db.getGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, db.getWidth(), db.getHeight());
        
        if(!awake){
            g.setColor(Color.DARK_GRAY);
            for(int i = 1; i < db.getWidth(); i+=2)
                g.fillRect(i, 0, 1, db.getHeight());
            for(int i = 1; i < db.getHeight(); i+=2)
                g.fillRect(0, i, db.getWidth(), 1);
        }
        
        g.setColor(Color.WHITE);
        for(int i = 0; i < cells.length; i++){
            for(int j = 0; j < cells[0].length; j++){
                if(cells[i][j] > 0)
                    db.setRGB(i, j, 0xFFFFFF);
            }
        }
        
        Graphics2D go = (Graphics2D)db2.getGraphics();
        go.drawImage(db, 0, 0, db2.getWidth(), db2.getHeight(), null);
        go.setColor(Color.WHITE);
        go.drawString(awake ? "Running" : "Paused| You can draw.", 4, 12);
        
        gf.drawImage(db2, 3, 25, 640, 640, null);
        //g2.drawImage(db, 3, 25, 640, 640, null);
        
        g.dispose();
        System.gc();
    }

    public static void Think(){
        //THINK HERE
        byte[][] tmp = new byte[cells.length][cells[0].length];
        boolean[][] chkd = new boolean[cells.length][cells[0].length];
        ArrayList<Point> activenew = new ArrayList<Point>();
        
        try{
            for(Point p : active){
                for(int x = p.x-1; x < p.x+2; x++)
                    for(int y = p.y-1; y < p.y+2; y++){
                        int xt = normalize(x, cells.length);
                        int yt = normalize(y, cells[0].length);

                        if(chkd[xt][yt] == true)
                            continue;
                        chkd[xt][yt] = true;

                        int num = GetNumAdjacent(xt, yt);
                        //RULES
                        if(num < 2 || num > 3)
                            tmp[xt][yt] = 0;
                        else if(num == 2)
                            tmp[xt][yt] = cells[xt][yt];
                        else
                            tmp[xt][yt] = 1;

                        if(tmp[xt][yt] > 0)
                            activenew.add(new Point(xt,yt));
                    }
            }
        }catch(ConcurrentModificationException ex){}
        
        
        active.clear();
        active = activenew;
        transcribe(tmp);
        System.gc();
    }
    
    public static int GetNumAdjacent(int x, int y){
        int c = 0;
        
        for (int i = x-1;i < x+2; i++)
            for(int j = y-1; j < y+2; j++){
                if(i == x && j == y)
                    continue;
                int it = normalize(i,cells.length);
                int jt = normalize(j,cells[0].length);
                if(cells[it][jt]>0)
                    c++;
            }
        
        return c;
    }
    
    public static void transcribe(byte[][] arr){
        for(int i = 0; i < cells.length; i++)
            for(int j = 0; j < cells[0].length; j++)
                cells[i][j] = arr[i][j];
    }
    
    public static int normalize(int a, int len){
        if(a >= len)
            return a-len;
        else if(a < 0)
            return len + a;
        return a;
    }

}

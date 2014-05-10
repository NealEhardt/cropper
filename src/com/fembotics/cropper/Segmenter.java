package com.fembotics.cropper;

import java.awt.*;
import java.awt.image.*;
import java.awt.geom.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.*;
import javax.swing.undo.*;

/**
 *
 * @author Neal Ehardt <nialsh@gmail.com>
 */
public class Segmenter {
    
    final BufferedImage image, overlay;
    final MainPanel mp;

    boolean[][] positive;
    
    final int W, H;
    
    final Pixel[][] pixelMatrix;
    final Raster raster;
    final Queue<PixelEvent> eventQueue = new ConcurrentLinkedQueue<PixelEvent>();
    
    public Segmenter(BufferedImage img, MainPanel mp) {
        image = img;
        this.mp = mp;
        
        W = img.getWidth();
        H = img.getHeight();
        raster = img.getRaster();

        overlay = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = overlay.createGraphics();
        g.setBackground(new Color(0, 0, 0, 0));
        g.clearRect(0, 0, W, H);
        
        pixelMatrix = new Pixel[H][W];
        for(int i = 0; i < H; ++i) {
            for(int j = 0; j < W; ++j)
                pixelMatrix[i][j] = new Pixel(j, i);
        }
    }


    private Pixel pixels(int x, int y) {
        return pixelMatrix[y][x];
    }
    private boolean positive(Pixel p) {
        return positive[p.y][p.x];
    }
    
    public boolean isOutOfDate() {
        return !eventQueue.isEmpty();
    }
    
    /** invalid locations will be safely ignored */
    public UndoableEdit addControlPoint(int x, int y, boolean positive)
    {
        if(x < 0 || x >= W || y < 0 || y >= H)
            return null;
        
        Pixel p = pixels(x, y);
        Boolean oldPos = null;
        if(p.isRoot())
            oldPos = p.positive;
        PixelEdit edit = new PixelEdit(p, oldPos, positive);
        edit.execute(true);
        return edit;
    }
    
    
    class PixelEvent {
        Pixel p;
        Boolean pos;
        public PixelEvent(Pixel p, Boolean pos) {
            this.p = p;
            this.pos = pos;
        }
    }
    
    

    static int[] GREEN = new int[] {0, 255, 0, 100};
    static int[] RED = new int[] {255, 0, 0, 100};
    static int[] BLUE = new int[] {0, 0, 255, 255};
    static int[] TRANSPARENT = new int[] {0, 0, 0, 0};
    public BufferedImage getOverlay()
    {
        return overlay;
    }


    public BufferedImage getCropped() {
        BufferedImage cropped = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        WritableRaster wr = cropped.getRaster();
        image.copyData(wr);

        for(int y = 0; y < H; ++y)
            for(int x = 0; x < W; ++x)
            {
                if(positive[y][x])
                    wr.setSample(x, y, 3, 255);
                else
                    wr.setSample(x, y, 3, 0); // set alpha to 0
            }

        return cropped;
    }

    public BufferedImage getBW() {
        BufferedImage bw = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        WritableRaster wr = bw.getRaster();


        final int[] white = new int[] {255, 255, 255, 255};
        final int[] black = new int[] {0, 0, 0, 255};

        for(int y = 0; y < H; ++y)
            for(int x = 0; x < W; ++x)
            {
                if(positive[y][x])
                    wr.setPixel(x, y, white);
                else
                    wr.setPixel(x, y, black);
            }

        return bw;
    }

    public boolean[][] getBinary() {
        boolean[][] b = new boolean[H][W];
        for(int y = 0; y < H; ++y)
            for(int x = 0; x < W; ++x) {
                Pixel p = pixels(x, y);
                b[y][x] = p.root == null || p.root.positive;
            }
        return b;
    }
    
    public Path2D getEdgePath() {
        final Path2D.Double path = new Path2D.Double();
        final HashSet<Pixel> visited = new HashSet<Pixel>();

        class WallFollower {
            
            /** returns true if it hits the edge of the screen;
             * returns false at the end of a closed loop */
            boolean trace(int xStart, int yStart, boolean hugLeft, boolean startDown) {
                
                /*
                 * The trace coordinate (x, y) is bounded by the following Pixels:
                 * (x-1, y-1) NW
                 * (x, y-1)   NE
                 * (x-1, y)   SW
                 * (x, y)     SE
                 */
                
                int d = startDown ? 1 : 3;
                int x = xStart;
                int y = yStart;
                path.moveTo(x, y);

                boolean first = true;

                for( ; ; ) {
                    if(x == xStart && y == yStart) { // cycle complete!
                        if(!first) {
                            path.lineTo(x, y);
                            return false;
                        }
                    } else
                        first = false;
                    
                    
                    Pixel primary = getNeighbor(x, y, d, hugLeft);
                    
                    if(primary == null) { // fell off the edge of the image
                        path.lineTo(x, y);
                        return true;
                    }
                    if(positive(primary)) { // turned an outside (convex) corner
                        if(hugLeft)
                            d = (d-1+4)%4;
                        else
                            d = (d+1)%4;
                        path.lineTo(x, y);
                        continue;
                    }


                    Pixel secondary = getNeighbor(x, y, d, !hugLeft);
                    
                    if(secondary == null) { // fell off the edge of the image
                        path.lineTo(x, y);
                        return true;
                    }
                    if(!positive(secondary)) { // turned an inside (concave) corner
                        if(hugLeft)
                            d = (d+1)%4;
                        else
                            d = (d-1+4)%4;
                        path.lineTo(x, y);
                        continue;
                    }

                    // going straight
                    switch(d) {
                        case 0:
                            x++; break;
                        case 1:
                            visited.add(pixels(x,y));
                            y++;
                            break;
                        case 2:
                            x--; break;
                        case 3:
                            y--;
                            visited.add(pixels(x,y));
                            break;
                        default:
                            throw new IllegalStateException();
                    }
                }
            }
            
            /*    * 3
             *    2   0
             *      1
             */
            
            Pixel getNeighbor(int x, int y, int d, boolean toLeft) {
                if(toLeft) {
                    if(d == 0 || d == 3)
                        y--;
                    if(d == 2 || d == 3)
                        x--;
                } else {
                    if(d == 1 || d == 2)
                        x--;
                    if(d == 2 || d == 3)
                        y--;
                }
                
                if(x >= 0 && y >= 0 && x < W && y < H)
                    return pixels(x, y);
                return null;
            }
        }
        WallFollower follower = new WallFollower();

        for(int y = 0; y < H; ++y)
            for(int x = 1; x < W; ++x) {
                Pixel p = pixels(x,y);
                Pixel prev = pixels(x-1,y);
                if(positive(prev) != positive(p) && !visited.contains(p)) {
                    if(positive(prev)) { // white to black
                        if(follower.trace(p.x, p.y, false, false))
                            follower.trace(p.x, p.y, true, true);
                    } else { // black to white
                        if(follower.trace(p.x, p.y, false, true))
                            follower.trace(p.x, p.y, true, false);
                    }
                }
            }
        return path;
    }


    /** a queue that doesn't allow duplicates */
    Queue<Pixel> updaterQueue = new ArrayDeque<Pixel>() {

        HashSet<Pixel> set = new HashSet<Pixel>();

        @Override
        public boolean add(Pixel p) {
            if(set.add(p))
                return super.add(p);
            return false;
        }

        @Override
        public Pixel poll() {
            Pixel p = super.poll();
            set.remove(p);
            return p;
        }
    };

    class InfiniteLoopException extends RuntimeException {}
    
    /** Propagates the changes made by any newly added control points */
    public boolean update()
    {
        try {
            dequeueEvents();
            greedyExpansion();
        } catch(HeadlessException e) {
            return false;
        }

        mp.statusLabel.setText("Rendering...");
        positive = getBinary();
        return true;
    }
    
    private void dequeueEvents() {
        if(eventQueue.isEmpty())
            return;
        mp.statusLabel.setText("Merging in new events...");

        while(!eventQueue.isEmpty()) {
            PixelEvent e = eventQueue.poll();
            e.p.setPositive(e.pos);
        }
    }

    private static DecimalFormat commaAdder = new DecimalFormat("###,###");
    private void greedyExpansion() {
        int maxN = 0;

        int count = 0;
        ArrayList<Integer> sizeHistory = new ArrayList<Integer>();
        while(!updaterQueue.isEmpty()) {
            if(++count % 512 == 0) {
                if(!alive)
                    throw new InfiniteLoopException();

                // check for infinite cycles
                outer:
                for(int n = 1; n < sizeHistory.size()/10; ++n) {
                    for(int i = n; i < sizeHistory.size(); ++i)
                        if(sizeHistory.get(i) != sizeHistory.get(i%n))
                            continue outer;

                    System.err.println("Cycle found!! length="+n);
                    for(int i = 0; i < n; ++i)
                        System.err.print(sizeHistory.get(i)+", ");
                    System.err.println();
                    for(Pixel p : updaterQueue)
                        System.err.println(p);
                    System.err.println();
                    throw new HeadlessException();
                }
                sizeHistory.clear();
                
                dequeueEvents();

                int N = updaterQueue.size();
                maxN = Math.max(N, maxN);

                mp.statusLabel.setText("Propagating changes... queue size: "
                                                        +commaAdder.format(N));
                mp.progressBar.setValue(100*(maxN-N)/maxN);
            }

            sizeHistory.add(updaterQueue.size());

            updaterQueue.poll().tryToTakeOverNeighbors();
        }
    }
    
    
    class Pixel
    {
        final int x, y;

        Pixel root;
        boolean positive = true;

        private Pixel parent;
        
        public Pixel(int x, int y) {
            this.x = x;
            this.y = y;
        }
        
        public boolean isRoot() { return this == root; }


        public void setPositive(Boolean pos)
        {
            parent = null;

            updaterQueue.add(this);
            if(pos == null) {
                if(root != null)
                    setRoot(null);
            } else {
                positive = pos;
                if(root != this)
                    setRoot(this);
            }
        }

        private void setRoot(Pixel r) {
            root = r;
            for(Pixel p : neighbors) {
                if(p.parent == this)
                    p.setRoot(r);
                else
                    updaterQueue.add(p);
            }
        }
        
        

        public void tryToTakeOverNeighbors() {
            if(root == null)
                return;

            for(Pixel n : neighbors)
                if(n.isBetterParent(this)) {
                    n.parent = this;
                    n.setRoot(root);
                    updaterQueue.add(this);
                }
        }

        public int[] getColor() {
            return raster.getPixel(x, y, (int[])null);
        }
        
        private boolean isBetterParent(Pixel p)
        {
            if(root == p.root)
                return false;

            if(root == null)
                return true;

            return isFirstColorNearer(p.root.getColor(), root.getColor(), getColor());
        }

        final Iterable<Pixel> neighbors = new Iterable<Pixel>() {
            public Iterator<Pixel> iterator() {
                return new Iterator<Pixel>() {
                    int state = 0;

                    public boolean hasNext() {
                        while(true) {
                            switch(state) {
                                case 0:
                                    if(x == W-1) break;
                                    return true;
                                case 1:
                                    if(x == 0) break;
                                    return true;
                                case 2:
                                    if(y == 0) break;
                                    return true;
                                case 3:
                                    if(y == H-1) break;
                                    return true;
                                default:
                                    return false;
                            }
                            state++;
                        }
                    }

                    public Pixel next() {
                        switch(state++) {
                            case 0:
                                return pixels(x+1, y);
                            case 1:
                                return pixels(x-1, y);
                            case 2:
                                return pixels(x, y-1);
                            case 3:
                                return pixels(x, y+1);
                            default:
                                throw new IllegalStateException();
                        }
                    }

                    public void remove() {
                        throw new UnsupportedOperationException("No.");
                    }
                };
            }
        };
        
        @Override
        public String toString() {
            return "Pixel[x="+x+"; y="+y
                    +"; positive="+(root==null?"null":root.positive)
                    +"; color="+Integer.toHexString(image.getRGB(x, y))
                    +"; isRoot="+isRoot()+"]";
        }
    }

    static int[] colorDiff(int[] x, int[] y) {
        return new int[] { x[0] - y[0],
                           x[1] - y[1],
                           x[2] - y[2] };
    }
    static int colorDist(int[] x, int[] y)
    {
        int r = x[0] - y[0];
        int g = x[1] - y[1];
        int b = x[2] - y[2];
        return r*r + g*g + b*b;
    }

    /** compares well even if d(c1, baseline) == d(c2, baseline) */
    static boolean isFirstColorNearer(int[] c1, int[] c2, int[] baseline) {
        int d1 = colorDist(baseline, c1);
        int d2 = colorDist(baseline, c2);

        if(d1 == d2) {
            int[] a = colorDiff(baseline, c1);
            int[] b = colorDiff(baseline, c2);
            if(a[0] == b[0]) {
                if(a[1] == b[1]) {
                    if(a[2] == b[2])
                        return false; // the colors are identical... whatever
                    return a[2] < b[2];
                }
                return a[1] < b[1];
            }
            return a[0] < b[0];
        }
        return d1 < d2;
    }



    class PixelEdit extends AbstractUndoableEdit {

        Pixel p;
        Boolean oldPos, newPos;

        public PixelEdit(Pixel p, Boolean oldPositive, Boolean newPositive) {
            this.p = p;
            oldPos = oldPositive;
            newPos = newPositive;
        }
        
        public void execute(boolean forward) {
            //System.out.println("execute("+forward+")");
            Boolean b;
            if(forward)
                b = newPos;
            else
                b = oldPos;
            
            eventQueue.add(new PixelEvent(p, b));

            WritableRaster wr = overlay.getRaster();
            if(b == null)
                wr.setPixel(p.x, p.y, TRANSPARENT);
            else if(b)
                wr.setPixel(p.x, p.y, GREEN);
            else
                wr.setPixel(p.x, p.y, RED);
        }

        @Override
        public void undo() throws CannotUndoException {
            super.undo();
            execute(false);
        }

        @Override
        public void redo() throws CannotRedoException {
            super.redo();
            execute(true);
        }

        @Override
        public String getPresentationName() { return "brush stroke"; }
    }

    boolean alive = true;
    public void destroy() {
        alive = false;
    }
}

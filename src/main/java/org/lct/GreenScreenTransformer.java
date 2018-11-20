package org.lct;


import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamImageTransformer;
import com.github.sarxos.webcam.WebcamPanel;
import org.json.simple.parser.ParseException;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;


public class GreenScreenTransformer implements WebcamImageTransformer {

    private Webcam webcam;
    private BufferedImage img = null;
    private Backgrounds imgs;
    private FilterPick fPick = new FilterPick();
    private Config cfg;

    private GreenScreenTransformer(String imgPath, String cfgPath) throws IOException, ParseException, IllegalAccessException {
        cfg = new Config(cfgPath);
        if (cfg.camName.equalsIgnoreCase("default")) {
            webcam = Webcam.getDefault();
        } else {
            java.util.List<Webcam> allWebcams = Webcam.getWebcams();
            for (Webcam cam : allWebcams) {
                if (cam.getDevice().getName().contains(cfg.camName)) {
                    try {
                        cam.open();
                    } catch (Exception e) {
                        continue;
                    }
                    cam.close();
                    webcam = cam;
                    break;
                }
            }
        }
        if (webcam == null) {
            System.out.println("No suitable webcam could be found.");
            return;
        }
        Dimension cameraResolution = new Dimension(cfg.resWidth, cfg.resHeight);
        webcam.setCustomViewSizes(cameraResolution);
        webcam.setViewSize(cameraResolution);
        webcam.setImageTransformer(this);
        webcam.open();
        JFrame window = new JFrame("Chromakey");
        WebcamPanel panel = new WebcamPanel(webcam);
        panel.setFPSDisplayed(false);
        panel.setFillArea(true);
        window.setLayout(new FlowLayout(FlowLayout.CENTER));
        window.add(panel);
        window.pack();
        window.setVisible(true);
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.addMouseListener(new MouseListener() {
            public void mouseClicked(MouseEvent e) {
                fPick.pickX = e.getX();
                fPick.pickY = e.getY();
                fPick.pick = true;
            }
            public void mousePressed(MouseEvent mouseEvent){}
            public void mouseReleased(MouseEvent mouseEvent){}
            public void mouseEntered(MouseEvent mouseEvent){}
            public void mouseExited(MouseEvent mouseEvent){}
        });
        window.addKeyListener(new KeyListener() {
            public void keyTyped(KeyEvent e){}
            public void keyReleased(KeyEvent e){}
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == 32) {
                    img = imgs.nextImage();
                }
            }
        });
        loadImgs(imgPath);
    }

    public BufferedImage transform(BufferedImage image) {
        if (fPick.pick) {
            pickColor(image);
        }
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                int rgb = image.getRGB(x, y);
                float hsb[] = getHSBfromRGB(rgb);
                float deg = hsb[0] * 360;
                if (deg >= cfg.minHue && deg <= cfg.maxHue &&
                        hsb[1] > cfg.minSat && hsb[1] < cfg.maxSat &&
                        hsb[2] > cfg.minBri && hsb[2] < cfg.maxBri) {
                    if (img != null)
                        image.setRGB(x, y, img.getRGB(x, y));
                }
            }
        }
        return image;
    }

    private float[] getHSBfromRGB(int rgb){
        float hsb[] = new float[3];
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = (rgb) & 0xFF;
        Color.RGBtoHSB(r, g, b, hsb);
        return hsb;
    }

    private void pickColor(BufferedImage image) {
        int rgb = image.getRGB(fPick.pickX, fPick.pickY);
        float hsb[] = getHSBfromRGB(rgb);
        cfg.minHue = (hsb[0] * 360) - cfg.hueDelta;
        cfg.maxHue = (hsb[0] * 360) + cfg.hueDelta;
        cfg.minSat = hsb[1] - cfg.satDelta;
        cfg.maxSat = hsb[1] + cfg.satDelta;
        cfg.minBri = hsb[2] - cfg.briDelta;
        cfg.maxBri = hsb[2] + cfg.briDelta;
        fPick.pick = false;
        cfg.writeConfig();
    }

    class FilterPick {
        int pickX;
        int pickY;
        boolean pick = false;
    }

    class Backgrounds {
        java.util.List<BufferedImage> imgs = new LinkedList<>();
        private int idx = -1;

        BufferedImage nextImage() {
            idx++;
            if (idx <= imgs.size() - 1) {
                return imgs.get(idx);
            } else {
                idx = 0;
                return imgs.get(idx);
            }
        }
    }

    public static void main(final String[] args) {
        if (args.length !=2){
            System.out.println("Usage: java -jar greenscreen.jar /path/to/background/images /path/to/config.json");
            System.exit(1);
        }
        JFrame.setDefaultLookAndFeelDecorated(true);
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                    new GreenScreenTransformer(args[0], args[1]);
                } catch (Exception e) {
                    System.out.println("Internal error " + e.getMessage() + e.getCause());
                }
            }
        });
    }

    private void loadImgs(String path) throws IOException {
        imgs = new Backgrounds();
        File folder = new File(path);
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    imgs.imgs.add(ImageIO.read(file));
                    System.out.println("Load image: " + file.getName());
                }
            }
        }else {
            System.out.println("No images found in: " + path);
            System.exit(1);
        }
        img = imgs.nextImage();
    }
}
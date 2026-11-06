package org.PSmart.POS.Utils;

import java.awt.image.BufferedImage;

public abstract class ImageResultRunnable implements Runnable{

    private BufferedImage bufferedImage;

    public void setBufferedImage(BufferedImage bufferedImage) {
        this.bufferedImage = bufferedImage;
    }

    public BufferedImage getBufferedImage() {
        return bufferedImage;
    }
}

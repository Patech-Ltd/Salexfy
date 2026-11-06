package org.PSmart.POS.Utils;

import org.PSmart.POS.ui.Login.LoginWindowOld;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.HashMap;

public class IconsUtils {

    public static HashMap<String,BufferedImage> cache=new HashMap<>();

    public static BufferedImage getIcon(String name)
    {
        if(cache.containsKey(name))
            return cache.get(name);

        try {
            InputStream stream = LoginWindowOld.class.getResourceAsStream("/icons/" + name);
            BufferedImage tmp=ImageIO.read(stream);
            cache.put(name,tmp);
            return tmp;
        }catch (Exception e)
        {
//            e.printStackTrace();
            cache.put(name,null);
            return null;
        }
    }
}

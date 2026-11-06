package org.PSmart.POS.Entities;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.PSmart.POS.POSApp;
import org.PSmart.POS.Utils.ImageResultRunnable;

import javax.imageio.ImageIO;
import jakarta.persistence.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;


@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "ItemCategory")
public class ItemCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID uid;

    private String name;

    private String Description;

    private boolean deleted;





    public void setImage(File file)
    {

        final String saveName="category"+getUid();
        new Thread(()->{
            File folder=new File(POSApp.imagesSavePath+"categoryImage/");
            try{
                folder.mkdirs();
            }catch (Exception es)
            {
                es.printStackTrace();
            }

            try{
                File saveFile=new File(folder,saveName);
//                file.copy(saveFile);

                Files.copy(file.toPath(),new FileOutputStream(saveFile));
            }catch (Exception es)
            {
                es.printStackTrace();
            }
        }).start();


    }


    public void getImage(ImageResultRunnable runnable)
    {
        final String saveName="category"+getUid();
        new Thread(()->{
            try{
                File folder=new File(POSApp.imagesSavePath+"categoryImage/");
                File saveFile=new File(folder,saveName);
                if(saveFile.exists())
                {
                    BufferedImage bufferedImage= ImageIO.read(saveFile);
                    runnable.setBufferedImage(bufferedImage);
                    runnable.run();
                }

            }catch (Exception es)
            {
                es.printStackTrace();
            }

        }).start();
    }


    public String toString()
    {
        return getName();
    }

    public String getSearchString()
    {
        return (name+" "+getDescription()).toLowerCase();
    }


}

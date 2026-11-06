package org.PSmart.POS.Entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.PSmart.POS.POSApp;
import org.PSmart.POS.Utils.ImageResultRunnable;
import org.hibernate.annotations.IndexColumn;

import javax.imageio.ImageIO;
import jakarta.persistence.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.UUID;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Item {

        @Id
        @GeneratedValue(strategy = GenerationType.AUTO)
        private UUID uid;

        private String name;

        private String description;

        @Column(unique = true,nullable = true)
        private String barcode;

        private Double buyingPrice;

        private Double sellingPrice;

        @ManyToOne
        private ItemCategory category;

private Double reorderLevel;

    /** Expiry date as epoch milliseconds (nullable). Null = non-perishable. */
    private Long expiryDate;

    private String supplier;

        private Double vat;

        private boolean deleted;

        @OneToMany(mappedBy = "item")
        private WholeScale[] wholeScales;

        @OneToMany(mappedBy = "item")
        private SubScale[] subScales;

        public void setImage(File file)
        {

                final String saveName="item"+getUid();
                new Thread(()->{
                        File folder=new File(POSApp.imagesSavePath+"itemImage/");
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
                final String saveName="item"+getUid();
                new Thread(()->{
                        try{
                                File folder=new File(POSApp.imagesSavePath+"itemImage/");
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


        public void deleteImage(Item variable) {
                final String saveName="item"+getUid();
                new Thread(()->{
                        try{
                                File folder=new File(POSApp.imagesSavePath+"itemImage/");
                                File saveFile=new File(folder,saveName);
                                if(saveFile.exists())
                                {
                                        saveFile.delete();
                                }

                        }catch (Exception es)
                        {
                                es.printStackTrace();
                        }

                }).start();
        }

        public String getSearchString()
        {
                return (" "+name+" "+getDescription()+" "+barcode+" "+category+" "+supplier).toLowerCase();
        }
}

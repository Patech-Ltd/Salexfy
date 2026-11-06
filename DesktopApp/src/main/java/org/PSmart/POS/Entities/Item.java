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

        /*
         * Scale options. They used to be mapped as Java arrays, which this Hibernate version cannot load when
         * an item has none ("ResultSet is closed" on Sales / Purchases). The mapped fields are lazy Lists now (loaded
         * only when the scales are actually read, never nested inside another query); the
         * getWholeScales()/getSubScales() array API below keeps all existing code working unchanged.
         */
        @OneToMany(mappedBy = "item")
        @lombok.Getter(lombok.AccessLevel.NONE) @lombok.Setter(lombok.AccessLevel.NONE)
        @lombok.EqualsAndHashCode.Exclude @lombok.ToString.Exclude
        @lombok.Builder.Default
        private java.util.List<WholeScale> wholeScaleList = new java.util.ArrayList<>();

        @OneToMany(mappedBy = "item")
        @lombok.Getter(lombok.AccessLevel.NONE) @lombok.Setter(lombok.AccessLevel.NONE)
        @lombok.EqualsAndHashCode.Exclude @lombok.ToString.Exclude
        @lombok.Builder.Default
        private java.util.List<SubScale> subScaleList = new java.util.ArrayList<>();

        /** Working copies used by the item form (positions = levels); not stored. */
        @Transient
        @lombok.Getter(lombok.AccessLevel.NONE) @lombok.Setter(lombok.AccessLevel.NONE)
        @lombok.EqualsAndHashCode.Exclude @lombok.ToString.Exclude
        private WholeScale[] wholeScalesView;

        @Transient
        @lombok.Getter(lombok.AccessLevel.NONE) @lombok.Setter(lombok.AccessLevel.NONE)
        @lombok.EqualsAndHashCode.Exclude @lombok.ToString.Exclude
        private SubScale[] subScalesView;

        public WholeScale[] getWholeScales() {
                if (wholeScalesView != null) return wholeScalesView;
                try {
                        return wholeScaleList == null ? new WholeScale[0] : wholeScaleList.toArray(new WholeScale[0]);
                } catch (RuntimeException e) {      // session already closed: behave as "no scales"
                        return new WholeScale[0];
                }
        }

        public void setWholeScales(WholeScale[] scales) {
                this.wholeScalesView = scales;
        }

        public SubScale[] getSubScales() {
                if (subScalesView != null) return subScalesView;
                try {
                        return subScaleList == null ? new SubScale[0] : subScaleList.toArray(new SubScale[0]);
                } catch (RuntimeException e) {
                        return new SubScale[0];
                }
        }

        public void setSubScales(SubScale[] scales) {
                this.subScalesView = scales;
        }

        public static class ItemBuilder {
                public ItemBuilder wholeScales(WholeScale[] scales) {
                        this.wholeScalesView = scales;
                        return this;
                }

                public ItemBuilder subScales(SubScale[] scales) {
                        this.subScalesView = scales;
                        return this;
                }
        }

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

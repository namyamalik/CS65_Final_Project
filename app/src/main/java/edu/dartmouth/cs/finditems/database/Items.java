package edu.dartmouth.cs.finditems.database;

public class Items {
    private Long id;

    private String itemName, itemLocalisation, itemDate, itemTime, itemGpsData;
    private byte[] itemImage;


    public Items() {
        this.id = id;
        this.itemName = itemName;
        this.itemImage = itemImage;
        this.itemLocalisation = itemLocalisation;
        this.itemDate = itemDate;
        this.itemTime = itemTime;
        this.itemGpsData = itemGpsData;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public byte[] getItemImage() {
        return itemImage;
    }

    public void setItemImage(byte[] itemImage) {
        this.itemImage = itemImage;
    }

    public String getItemLocalisation() { return itemLocalisation; }

    public void setItemLocalisation(String itemLocalisation) { this.itemLocalisation = itemLocalisation; }

    public String getItemDate() { return itemDate; }

    public void setItemDate(String itemDate) { this.itemDate = itemDate; }

    public String getItemTime() { return itemTime; }

    public void setItemTime(String itemTime) { this.itemTime = itemTime; }

    public String getItemGpsData() {
        return itemGpsData;
    }
    public void setItemGpsData(String itemGpsData) {
        this.itemGpsData = itemGpsData;
    }
}


package model;

public final class PropertyTile extends Tile {

    private final int price;
    private final int baseRent;

    private Integer ownerId = null; // null = sahipsiz

    // ileride kullanacağız
    private int buildingLevel = 0; // 0..3
    private boolean hasHotel = false;

    public PropertyTile(int id, String name, int price, int baseRent) {
        super(id, name, TileType.PROPERTY);
        this.price = price;
        this.baseRent = baseRent;
    }

    public int price() { return price; }
    public int baseRent() { return baseRent; }

    public Integer ownerId() { return ownerId; }
    public boolean isOwned() { return ownerId != null; }
    public void setOwnerId(Integer ownerId) { this.ownerId = ownerId; }

    public int buildingLevel() { return buildingLevel; }
    public void setBuildingLevel(int lvl) { this.buildingLevel = Math.max(0, Math.min(3, lvl)); }

    public boolean hasHotel() { return hasHotel; }
    public void setHasHotel(boolean v) { this.hasHotel = v; }

    // şimdilik kira = baseRent (sonra festival/worldcup/otel ekleriz)
    public int rent() {
        return baseRent;
    }
}

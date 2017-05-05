package org.komparator.mediator.domain;


public class Item{
	private ItemId itemId;
    private String desc;
    private int price;
    
    public Item (ItemId itemId, String desc, int price){
    	this.itemId=itemId;
    	this.desc=desc;
    	this.price=price;
    	
    }
    public ItemId getItemId() {
		return itemId;
	}

	public void setItemId(ItemId itemId) {
		this.itemId = itemId;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	public int getPrice() {
		return price;
	}

	public void setPrice(int price) {
		this.price = price;
	}
	
	@Override
	public boolean equals(Object obj) {
		Item i = (Item) obj;
		return this.itemId.equals(i.getItemId()) && this.price == i.getPrice() && this.desc.equals(i.getDesc());
	}
    
}

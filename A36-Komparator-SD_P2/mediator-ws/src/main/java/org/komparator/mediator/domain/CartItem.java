package org.komparator.mediator.domain;


public class CartItem {
	private Item item;
    private int quantity;
    
    public CartItem(Item item, int quantity){
    	this.item=item;
    	this.quantity=quantity;
    }  
    public Item getItem() {
		return item;
	}

	public void setItem(Item item) {
		this.item = item;
	}

	public int getQuantity() {
		return quantity;
	}

	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}
	public void addQuantity(int quantity){
		this.quantity+=quantity;
	}
	public String getSupplier(){
		return this.item.getItemId().getSupplierId();
	}
	public String getproductId(){
		return this.item.getItemId().getProductId();
	}
	public int getPrice(){
		return this.item.getPrice();
	}
}


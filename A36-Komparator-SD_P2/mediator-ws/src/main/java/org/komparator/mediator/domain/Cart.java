package org.komparator.mediator.domain;

import java.util.ArrayList;
import java.util.List;



public class Cart {
	private String cartId;
    private List<CartItem> items= new ArrayList<CartItem>();
    
    public Cart(String cartId){
    	this.cartId=cartId;
    	
    }
	public void setCartId(String cartId) {
		this.cartId = cartId;
	}
	
    public void  addItem(CartItem item){
    	items.add(item);
    }
    public String getCartId() {
		return cartId;
	}
    public List<CartItem> getCartItems(){
    	return items;
    }
    
    public CartItem getItem(Item item){
    	for(CartItem cartItem: items){
    		//System.out.println("COMPARING " + item.getItemId().getProductId() + " WITH ");
    		if (cartItem.getItem().equals(item)){
    			return cartItem;
    		}
    	}
    	return null;
    }
    
}

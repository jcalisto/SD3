package org.komparator.mediator.domain;


import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.komparator.mediator.ws.ShoppingResultView;



public class Mediator {
	

	private Map<String, Cart> carts = new ConcurrentHashMap<>();
	private ArrayList<ShoppingResultView> purchases = new ArrayList<ShoppingResultView>();
	
	

	//////////////////////////  SINGLETON  /////////////////////////////////
	
	/* Private constructor prevents instantiation from other classes */
	private Mediator(){
	}
	
	/**
	 * SingletonHolder is loaded on the first execution of
	 * Singleton.getInstance() or the first access to SingletonHolder.INSTANCE,
	 * not before.
	 */
	private static class SingletonHolder {
		private static final Mediator INSTANCE = new Mediator();
	}

	public static synchronized Mediator getInstance() {
		return SingletonHolder.INSTANCE;
	}
	
	//////////////////////// END SINGLETON /////////////////////////////////
	
	//Clear Set of items

	public void reset(){
		carts.clear();
	}
	
	public Boolean itemExists(String itemId) {
		return carts.containsKey(itemId);
	}
	
	public Cart getCart(String cartId) {
		return carts.get(cartId);
	}
	
	public void addCart(Cart cart){
		if(cart != null)
			carts.put(cart.getCartId(), cart);
	}
	public ArrayList<Cart> getArrayCarts(){
		return new ArrayList<Cart>(carts.values());
	}
	public ArrayList<ShoppingResultView> getArrayPurchases(){
		return purchases;
	}
	public void addShoppingResult(ShoppingResultView s){
		purchases.add(s);
	}
}

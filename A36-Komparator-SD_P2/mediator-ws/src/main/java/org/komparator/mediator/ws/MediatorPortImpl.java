package org.komparator.mediator.ws;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;


import javax.jws.WebService;

import org.komparator.mediator.domain.Cart;
import org.komparator.mediator.domain.CartItem;
import org.komparator.mediator.domain.Item;
import org.komparator.mediator.domain.ItemId;
import org.komparator.mediator.domain.Mediator;

import org.komparator.supplier.ws.BadProductId_Exception;
import org.komparator.supplier.ws.BadQuantity_Exception;
import org.komparator.supplier.ws.BadText_Exception;
import org.komparator.supplier.ws.InsufficientQuantity_Exception;
import org.komparator.supplier.ws.ProductView;
import org.komparator.supplier.ws.cli.SupplierClient;
import org.komparator.supplier.ws.cli.SupplierClientException;


import pt.ulisboa.tecnico.sdis.ws.cli.CreditCardClient;
import pt.ulisboa.tecnico.sdis.ws.cli.CreditCardClientException;
import pt.ulisboa.tecnico.sdis.ws.uddi.UDDINamingException;
import pt.ulisboa.tecnico.sdis.ws.uddi.UDDIRecord;

@WebService(
		endpointInterface = "org.komparator.mediator.ws.MediatorPortType", 
		wsdlLocation = "mediator.wsdl", 
		name = "Mediator", 
		portName = "MediatorPort", 
		targetNamespace = "http://ws.mediator.komparator.org/", 
		serviceName = "MediatorService"
)
public class MediatorPortImpl implements MediatorPortType{

	// end point manager
	private MediatorEndpointManager endpointManager;
	//private ArrayList<SupplierClient> listSuppliers=new ArrayList<SupplierClient>();
	private List<SupplierClient> listSuppliers=new ArrayList<SupplierClient>();
	private AtomicInteger purchaseIdCounter = new AtomicInteger(0);
	private static final String A36_SUPPLIER="A36_Supplier";
	


	public MediatorPortImpl(MediatorEndpointManager endpointManager) {
		this.endpointManager = endpointManager;
	}
	
	
	// Main operations -------------------------------------------------------

	//Clear system state
	@Override
	public synchronized void clear() {
		ping("Hello");                                    //Actualizar lista de suppliers
		for(SupplierClient supplierClient : listSuppliers)
			supplierClient.clear();
		Mediator mediator = Mediator.getInstance();
		mediator.reset();
		listSuppliers.clear();
	}
	public List<UDDIRecord> findSuppliers(){
		List<UDDIRecord> supplierClientsRecords =new ArrayList<UDDIRecord>();
		try {
			supplierClientsRecords=(List<UDDIRecord>)endpointManager.getUddiNaming().listRecords(A36_SUPPLIER+"%");
		} catch (UDDINamingException e) {
			System.out.println("Error in find Suppliers");
		}
		return supplierClientsRecords;
	}
	
	
	//Find productId in every supplier
	@Override
	public synchronized List<ItemView> getItems(String productId) throws InvalidItemId_Exception {
		if(productId == null || productId.trim().equals(""))
			InvalidItemId_ExceptionThrow("Error itemId\n");
		ping("Hello");											//Atualizar lista de suppliers
		List<ItemView> itemList=new ArrayList<ItemView>();
		try{
			for(SupplierClient cl:listSuppliers){
				ProductView productView= cl.getProduct(productId);		//Check if suppliers has the product
				if(productView!=null){
					ItemView item=newProdToItemView(productView, cl.getName());
					itemList.add(item);
				}
			}
		}catch(BadProductId_Exception e){
			InvalidItemId_ExceptionThrow("Error itemId\n");
		}
		itemList.sort(new Comparator<ItemView>(){					//Sort List by price
			public int compare(ItemView i1, ItemView i2){
				return i1.getPrice() - i2.getPrice();
			}
		});
		return itemList;
	}
	
	//List carts in mediator
	@Override
	public synchronized List<CartView> listCarts() {
		Mediator mediator=Mediator.getInstance();
		ArrayList<CartView> cartViewList=new ArrayList<>();
		for(Cart cart:mediator.getArrayCarts()){
			CartView cartView=newCartView(cart);
			cartViewList.add(cartView);
		}
		return cartViewList;
	}

	@Override
	public  synchronized List<ItemView> searchItems(String descText) throws InvalidText_Exception{
		ping("Hello");                                                                    //Actualizar lista de suppliers
		List<ItemView> items = new ArrayList<ItemView>();                                 
		for(SupplierClient supplierClient : listSuppliers){                               //Percorrer Lista de Suppliers
			try {
				List<ProductView> products = supplierClient.searchProducts(descText);     //Produtos de um Supplier com a descricao
				for(ProductView p : products){
					items.add(newProdToItemView(p, supplierClient.getName()));            //Transformar produto em itemview
				}
			}
			catch(BadText_Exception e){
				throwInvalidTextException("Invalid Description on search items");
			}
		}
		
		items.sort(new Comparator<ItemView>(){								//Sort List by ID and Price
			@Override
			public int compare(ItemView item1, ItemView item2){
				int value = item1.getItemId().getProductId().compareTo(item2.getItemId().getProductId());
				if(value == 0)
					return item1.getPrice() - item2.getPrice();
				return value;
			}
		});
		
		return items;
	}

	//buy products in the cart
	@Override
	public synchronized ShoppingResultView buyCart(String cartId, String creditCardNr)
			throws EmptyCart_Exception, InvalidCartId_Exception, InvalidCreditCard_Exception{
		try{

			if (cartId==null||cartId.trim().equals("")){
				InvalidCartId_ExceptionThrow("Invalid cartId");
			}
			if (creditCardNr==null||creditCardNr.trim().equals("")){
				 CreditCardClientExceptionThrow("Invalid creditCardNr");
			}

			Mediator mediator=Mediator.getInstance();
			int totalPrice=0;
			List<CartItemView> purchasedItems=new ArrayList<CartItemView>();
			List<CartItemView> droppedItems=new ArrayList<CartItemView>();
			CreditCardClient client =new CreditCardClient("http://ws.sd.rnl.tecnico.ulisboa.pt:8080/cc"); //comunicate with bank
			Result result=Result.COMPLETE;
			int purchaseId = purchaseIdCounter.incrementAndGet(); //create ID
			if(!client.validateNumber(creditCardNr)){ //if credit card not valid throw exception
				CreditCardClientExceptionThrow("Error with creditCart");
			}
			Cart cart=mediator.getCart(cartId);
			if (cart==null){
				InvalidCartId_ExceptionThrow("Invalid cartId"); //if cart null throws exception
			}
			if (cart.getCartItems().size()==0){
				EmptyCart_ExceptionThrow("Empty Cart"); //if cart is empty then throws exception
			}
			for(CartItem cartItem: cart.getCartItems()){
				String supplierID=cartItem.getSupplier();
				CartItemView view=newCartItemView(cartItem);
				try{
					SupplierClient supplierClient = new SupplierClient(this.endpointManager.getUddiNaming().getUDDIUrl(),supplierID);
					if(supplierClient.getWsURL() == null)
						InvalidCartId_ExceptionThrow("CartId com SupplierID errado"); 
					supplierClient.buyProduct(cartItem.getproductId(), cartItem.getQuantity()); //try buy products
					purchasedItems.add(view);//add to purchased if all is ok
					totalPrice+=cartItem.getPrice()*cartItem.getQuantity();//add to total ammount 
				}catch (BadProductId_Exception e){
					droppedItems.add(view);
					result=Result.PARTIAL;
				} catch (BadQuantity_Exception e) {
					droppedItems.add(view);
					result=Result.PARTIAL;
				} catch (InsufficientQuantity_Exception e) {
					droppedItems.add(view);
					result=Result.PARTIAL;
				
				}catch(SupplierClientException e){
					droppedItems.add(view);
					result=Result.PARTIAL;
				}
			}
			if (purchasedItems.size()==0){
					result=Result.EMPTY;
			}
			ShoppingResultView shop=newShowResultView(Integer.toString(purchaseId),result,purchasedItems,droppedItems,totalPrice);// create ShoppingResultView with all the necessary arguments
			mediator = Mediator.getInstance();
			mediator.addShoppingResult(shop);//add to list in mediator
			return shop;
		}catch( CreditCardClientException e){
			CreditCardClientExceptionThrow("Error with your credit card");
		}
		return null;
}
	
	//add items to the cart
	@Override
	public synchronized void addToCart(String cartId, ItemIdView itemId, int itemQty) throws InvalidCartId_Exception,
			InvalidItemId_Exception, InvalidQuantity_Exception, NotEnoughItems_Exception {
		ping("Hello");
		try{
			if(cartId==null || cartId.trim().equals("")){
				invalidCarIdExceptionThrow("Invalid car arguments");	
			}
			if(itemId==null || itemId.getProductId()==null || itemId.getSupplierId()==null){
				InvalidItemId_ExceptionThrow("Invalid car arguments");	
			}
			if(itemQty<=0){
				InvalidQuantity_ExceptionThrow("Invalid quantity");
			}
			Mediator mediator=Mediator.getInstance();
			String supplierID=itemId.getSupplierId(); //get supplier
			SupplierClient supplierClient = new SupplierClient(this.endpointManager.getUddiNaming().getUDDIUrl(),supplierID);
			if(supplierClient.getWsURL()==null)
				InvalidItemId_ExceptionThrow("Invalid SupplierId");	
			ProductView p= supplierClient.getProduct(itemId.getProductId());
			
			if(p==null)
				InvalidItemId_ExceptionThrow("Invalid ProductId");	
			if (itemQty>p.getQuantity()){// verify if quantity is acceptable
				NotEnoughItems_ExceptionThrow("Quantity is higher than quantity available");
			}
			String desc=supplierClient.getProduct(itemId.getProductId()).getDesc();// get description
			int price=supplierClient.getProduct(itemId.getProductId()).getPrice(); //get price
			ItemId itemId2=new ItemId(itemId.getProductId(),itemId.getSupplierId());
			Item item=new Item(itemId2,desc,price);//create item
			Cart cart=mediator.getCart(cartId);//check if cart exists
			
			if (cart==null){
				cart=new Cart(cartId);//create cart if it does not exists
				CartItem cartItem=new CartItem(item, itemQty);
				cart.addItem(cartItem);
				mediator.addCart(cart);//add cart to list in mediator if does not exists
			}
			else{
				CartItem cartItem=cart.getItem(item); 
				if(cart.getItem(item)==null){ //create item if it does not exists
					cartItem=new CartItem(item, itemQty);
					cart.addItem(cartItem);
				}
				else{
					cartItem.addQuantity(itemQty);
				}
			
			}
			
		}catch(SupplierClientException e){
			InvalidItemId_ExceptionThrow("Error in Supplier");
		
		} catch (BadProductId_Exception e) {
			InvalidItemId_ExceptionThrow("Bad Product Id");
		}
	}
		
	@Override
	public synchronized  String ping(String message) {
		String result = ""; 
		for(UDDIRecord r: findSuppliers()){
			SupplierClient supplierClient;
			try {
				supplierClient = new SupplierClient(r.getUrl());
				supplierClient.setUddiUrl(endpointManager.getUddiNaming().getUDDIUrl());
				supplierClient.setName(r.getOrgName());
				listSuppliers.add(supplierClient);							//Found supplier, add it to list of suppliers
				result += "\n" + supplierClient.ping(message);
			} catch (SupplierClientException e) {
				System.out.println("Error in suplierClient");
			}
		}
		return result;
	}

	@Override
	public synchronized List<ShoppingResultView> shopHistory() {
		Mediator mediator= Mediator.getInstance();
		return mediator.getArrayPurchases();
	}

	
	// View helpers -----------------------------------------------------
	
	//Create ItemView from Item
	private ItemView newItemView(Item item) {
		ItemView view = new ItemView();
		ItemIdView id=newItemIdView(item.getItemId());		
		view.setItemId(id);
		view.setDesc(item.getDesc());
		view.setPrice(item.getPrice());
		return view;
	}
	
	//Create temIdView  from ItemId
	private ItemIdView newItemIdView(ItemId item){
		ItemIdView view=new ItemIdView();
		view.setProductId(item.getProductId());
		view.setSupplierId(item.getSupplierId());
		return view;
	}
	
	//Create CartItemView  from CartItem
	private CartItemView newCartItemView(CartItem cartItem){
		CartItemView view= new CartItemView();
		ItemView viewItem = newItemView(cartItem.getItem());
		view.setItem(viewItem);
		view.setQuantity(cartItem.getQuantity());
		return view;	
	}
	//Create CartView  from Cart
	private CartView newCartView(Cart cart){
		CartView view= new CartView();
		view.setCartId(cart.getCartId());;
		for(CartItem item: cart.getCartItems()){
			CartItemView itemView=newCartItemView(item);
			view.getItems().add(itemView);
		}
		return view;	
	}

	
	//Create new ItemView from ProductView
	private ItemView newProdToItemView(ProductView p, String supplierId){
		ItemView item = new ItemView();
		item.setDesc(p.getDesc());
		ItemIdView itemIdView= new  ItemIdView();
		itemIdView.setProductId(p.getId());
		itemIdView.setSupplierId(supplierId);
		item.setItemId(itemIdView);
		item.setPrice(p.getPrice());
		return item;
	}
	//Create ShoppingResultView
	private ShoppingResultView newShowResultView(String id,Result result,List<CartItemView> purchasedItems,List<CartItemView> droppedItems,int totalPrice){
		ShoppingResultView view=new ShoppingResultView();
		view.setId(id);
		view.setResult(result);
		view.setTotalPrice(totalPrice);
		for(CartItemView c: purchasedItems){
			view.getPurchasedItems().add(c);
		}
		for(CartItemView c2: droppedItems){
			view.getDroppedItems().add(c2);

		}
		return view;
	}

    
	// Exception helpers -----------------------------------------------------

    private InvalidCreditCard_Exception CreditCardClientExceptionThrow(String msg) throws InvalidCreditCard_Exception{
    	throw new InvalidCreditCard_Exception(msg, null);
    }
    
    private InvalidCartId_Exception InvalidCartId_ExceptionThrow(String msg) throws InvalidCartId_Exception{
    	throw new InvalidCartId_Exception(msg, null);
    }
    
    private EmptyCart_Exception EmptyCart_ExceptionThrow(String msg) throws EmptyCart_Exception{
    	throw new EmptyCart_Exception(msg, null);
    }
    public InvalidText_Exception throwInvalidTextException(String message) throws InvalidText_Exception{
    	InvalidText faultInfo = new InvalidText();
    	faultInfo.setMessage(message);
    	throw new InvalidText_Exception("Invalid Text: ", faultInfo);
    }
    
    private InvalidCartId_Exception invalidCarIdExceptionThrow(String msg) throws InvalidCartId_Exception{
    	throw new InvalidCartId_Exception(msg,null);

    }
    private InvalidItemId_Exception InvalidItemId_ExceptionThrow(String msg) throws InvalidItemId_Exception{
    	throw new InvalidItemId_Exception(msg,null);
    }
    private InvalidQuantity_Exception InvalidQuantity_ExceptionThrow(String msg) throws InvalidQuantity_Exception{
    	throw new InvalidQuantity_Exception(msg,null);
    }
    private NotEnoughItems_Exception NotEnoughItems_ExceptionThrow(String msg) throws InvalidQuantity_Exception{
    	throw new InvalidQuantity_Exception(msg,null);
    }

}

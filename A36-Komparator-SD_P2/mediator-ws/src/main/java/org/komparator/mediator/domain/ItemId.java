package org.komparator.mediator.domain;

public class ItemId {

	protected String productId;
    protected String supplierId;
    
    public ItemId(String prodId, String suppId){
    	this.productId = prodId;
    	this.supplierId = suppId;
    }
    
	public String getProductId() {
		return productId;
	}

	public void setProductId(String productId) {
		this.productId = productId;
	}

	public String getSupplierId() {
		return supplierId;
	}

	public void setSupplierId(String supplierId) {
		this.supplierId = supplierId;
	}
	
	@Override
	public boolean equals(Object obj) {
		ItemId i = (ItemId) obj;
		return this.productId.equals(i.getProductId()) && this.supplierId.equals(i.getSupplierId());
	}

	


}

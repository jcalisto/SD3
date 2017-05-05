package org.komparator.supplier.ws;


/** Main class that starts the Supplier Web Service. */
public class SupplierApp {

	public static void main(String[] args) throws Exception {
		String uddiURL;
		String name;
		String url;
		if (args.length==0 || args.length==2 ||args.length>3){
			System.out.println("Number of arguments invalid");
			return;
		}
		SupplierEndpointManager endpoint = null;
		System.out.println(args[0]);
		if (args.length == 1) {
			url = args[0];
			endpoint = new SupplierEndpointManager(url);
			endpoint.setVerbose(false);
		} else if (args.length >= 3) {
			uddiURL = args[0];
			name = args[1];
			url = args[2];
			System.out.println(args[0]+ " "+args[1]+" "+args[2]);
			endpoint = new SupplierEndpointManager(uddiURL, name, url);
			endpoint.setVerbose(true);
		}

		try {
			endpoint.start();
			endpoint.awaitConnections();
		} finally {
			endpoint.stop();
		}

	}

}

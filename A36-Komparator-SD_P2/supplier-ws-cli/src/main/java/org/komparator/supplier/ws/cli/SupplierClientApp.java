package org.komparator.supplier.ws.cli;

/** Main class that starts the Supplier Web Service client. */
public class SupplierClientApp {

	public static void main(String[] args) throws Exception {
		// Check arguments
		if (args.length < 1 || args.length>=3) {
			System.err.println("Argument(s) missing!");
			System.err.println("Usage: java " + SupplierClientApp.class.getName() + " wsURL");
			return;
		}
		System.out.println("sfsk");
		SupplierClient client=null;
		if (args.length==1){
			String url = args[0];
			client = new SupplierClient(url);
		}
		if (args.length==2){
			String uddiURL = args[0];
			String name = args[1];
			client = new SupplierClient(uddiURL,name);
			System.out.printf("Creating client for server at %s%n", uddiURL);
		}
		if (client==null){
			System.out.println("Supplier Client NULL");
			return;
		}

		System.out.println("Invoke ping()...");
		String result = client.ping("client");
		System.out.print("Result: ");
		System.out.println(result);
	}

}

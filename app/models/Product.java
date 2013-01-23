package models;

import java.util.*;

public class Product {

	public Long id;
	public String name;
	public String image;

	public Product(Long id, String name, String image) {
		this.name = name;
		this.id = id;
		this.image = image;
	}

	public static List<Product> all() {
		List<Product> products = new ArrayList<Product>();
		products.add(new Product(1l, "Soft Kitty Singing Plush", "http://a.tgcdn.net/images/products/category/ea67_soft_kitty_singing_plush.jpg"));
		products.add(new Product(2l, "Whiskey Stones", "http://a.tgcdn.net/images/products/category/ba37_whiskey_stones2.jpg"));
		products.add(new Product(3l, "USB Toast Handwarmers", "http://a.tgcdn.net/images/products/category/ebcc_usb_toast_handwarmers.jpg"));
		return products;
	}

	public static Product byId(Long id) {
		for(Product p: all()) {
			if(id == p.id) return p;
		}
		return null;
	}

}
package controllers;

import play.*;
import play.mvc.*;

import static play.data.Form.*;

import models.*;

import static views.Freemarker.*;

public class Application extends Controller {
  
	public static Result index() {
		return ok(
			view("index.ftl",
				_("user", session("user")),
				_("products", Product.all())
			)
		);
	}

	public static Result product(Long id) {
		Product product = Product.byId(id);
		if(product == null) {
			return notFound();
		} else {
			return ok(
				view("product.ftl",
					_("user", session("user")),
					_("product", product)
				)
			);
		}
	}

	// --

	public static Result login() {
		String user = form().bindFromRequest().get("user");
		session("user", user);
		return redirect(routes.Application.index());
	}

	public static Result logout() {
		session().clear();
		return redirect(routes.Application.index());
	}
  
}
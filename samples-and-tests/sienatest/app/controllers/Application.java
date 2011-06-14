package controllers;

import java.util.List;

import models.Employee;
import play.cache.Cache;
import play.mvc.After;
import play.mvc.Before;
import play.mvc.Controller;
import siena.Query;

public class Application extends Controller {
	static Query<Employee> q;

	@Before
	public static void loadQuery() {
		q = (Query<Employee>) Cache.get("q");
		if (q == null) {
			// stateful is just meant to use GAE cursors instead of offsets
			q = Employee.all().stateful().paginate(100);
		}
	}

	@After
	public static void saveQuery() {
		Cache.set("q", q);
	}

	public static void index() {
		List<Employee> emps = q.fetch();

		renderTemplate("Application/list.html", emps);
	}

	public static void nextPage() {
		List<Employee> emps = q.nextPage().fetch();

		renderTemplate("Application/list.html", emps);
	}

	public static void previousPage() {
		List<Employee> emps = q.previousPage().fetch();

		renderTemplate("Application/list.html", emps);
	}

}
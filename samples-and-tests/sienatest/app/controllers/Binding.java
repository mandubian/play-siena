package controllers;

import models.UUIDModel;
import play.mvc.Controller;

/**
 * User: Wursteisen David
 * Date: 17/12/11
 * Time: 17:21
 */
public class Binding extends Controller {
    public static void bindMe(UUIDModel myModel) {
        renderTemplate("Application/index.html");
    }
}

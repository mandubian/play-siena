package play.modules.siena;

import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import play.Logger;
import play.classloading.ApplicationClasses.ApplicationClass;
import play.classloading.enhancers.Enhancer;

/**
 * This class uses the Play framework enhancement process to enhance classes
 * marked with the morphia annotations.
 * 
 * @author mandubian <pascal.voitot@mandubian.org>
 *
 */
public class SienaEnhancer extends Enhancer{
	@Override
	public void enhanceThisClass(ApplicationClass applicationClass)
			throws Exception {
		enhanceThisClass_(applicationClass);
	}
	
	private void enhanceThisClass_(ApplicationClass applicationClass) throws Exception {
        final CtClass ctClass = makeClass(applicationClass);
        
        // enhances only EnhancedModel classes
        if (!ctClass.subtypeOf(classPool.get(EnhancedModel.class.getName()))) {
            return;
        }
        
        String entityName = ctClass.getName();
        
        Logger.debug("Play-Siena: enhancing EnhancedModel " + entityName);
        
        // all
        try {
        	ctClass.getDeclaredMethod("all");
        }catch(NotFoundException ex){
            CtMethod all = CtMethod.make("public static play.modules.siena.QueryWrapper all() { return new play.modules.siena.QueryWrapper(siena.Model.all("+entityName+".class)); }", ctClass);
            ctClass.addMethod(all);        	
        }
  
        // batch
        try {
        	ctClass.getDeclaredMethod("batch");
        }catch(NotFoundException ex){
        	CtMethod batch = CtMethod.make("public static play.modules.siena.BatchWrapper batch() { return new play.modules.siena.BatchWrapper(siena.Model.batch("+entityName+".class)); }", ctClass);
        	ctClass.addMethod(batch);
        }

        // create
        CtMethod create = CtMethod.make("public static play.modules.siena.EnhancedModel create(String name, play.mvc.Scope.Params params) { return play.modules.siena.EnhancedModel.create("+entityName+".class, name, params.all()); }",ctClass);
        ctClass.addMethod(create);

        // count
        CtMethod count = CtMethod.make("public static long count() { return (long)siena.Model.all("+entityName+".class).count(); }", ctClass);
        ctClass.addMethod(count);

        // findAll
        CtMethod findAll = CtMethod.make("public static java.util.List findAll() { return (java.util.List)siena.Model.all("+entityName+".class).fetch(); }", ctClass);
        ctClass.addMethod(findAll);

        // deleteAll
        CtMethod deleteAll = CtMethod.make("public static int deleteAll() { return siena.Model.all("+entityName+".class).delete(); }", ctClass);
        ctClass.addMethod(deleteAll);
  
        // findById
        CtMethod findById = CtMethod.make("public static "+entityName+" findById(Object id) { return ("+entityName+")siena.Model.all("+entityName+".class).getByKey(id); }", ctClass);
        ctClass.addMethod(findById);
        
        // Done.
        applicationClass.enhancedByteCode = ctClass.toBytecode();
        ctClass.detach();
	}

}

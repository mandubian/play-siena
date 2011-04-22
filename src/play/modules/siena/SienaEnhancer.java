package play.modules.siena;

import javassist.CtClass;
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
	
	void enhanceThisClass_(ApplicationClass applicationClass) throws Exception {
        // this method will be called after configuration finished
        // if (!MorphiaPlugin.configured()) return;

        final CtClass ctClass = makeClass(applicationClass);
	}
	
}

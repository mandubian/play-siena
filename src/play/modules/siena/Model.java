/**
 * 
 */
package play.modules.siena;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import play.Logger;
import play.Play;
import play.data.binding.BeanWrapper;
import play.data.binding.Binder;
import play.data.validation.Validation;
import play.exceptions.UnexpectedException;
import play.mvc.Scope.Params;
import siena.ClassInfo;
import siena.Filter;
import siena.Json;
import siena.SienaException;
import siena.core.batch.Batch;
import siena.embed.Embedded;

import com.google.gson.JsonParseException;

/**
 * Bridge between siena.Model and play.db.Model
 * 
 * @author mandubian <pascal.voitot@mandubian.org>
 *
 */
public class Model extends siena.Model implements Serializable, play.db.Model {
	private static final long serialVersionUID = 949918995355310821L;

	/* (non-Javadoc)
	 * @see play.db.Model#_save()
	 */
	@Override
	public void _save() {
		this.save();
	}

	/* (non-Javadoc)
	 * @see play.db.Model#_delete()
	 */
	@Override
	public void _delete() {
		this.delete();
	}

	/* (non-Javadoc)
	 * @see play.db.Model#_key()
	 */
	@Override
	public Object _key() {
		return siena.Util.readField(this, ClassInfo.getClassInfo(getClass()).getIdField());
	}

	public static <T extends Model> T create(Class<T> type, String name,
			Map<String, String[]> params) {
		return (T) create(type, name, params, new Annotation[0]);
	}
	
	public static <T extends Model> T create(Class<T> type, String name,
			Map<String, String[]> params, Annotation[] annotations) {
		T model = siena.Util.createObjectInstance(type);
		return (T) edit(model, name, params, annotations);
	}
	
    @SuppressWarnings("unchecked")
	public <T extends Model> T edit(String name, Map<String, String[]> params) {
        edit(this, name, params, new Annotation[0]);
        return (T) this;
    }
	
	public static <T extends Model> T edit(T o, String name, Map<String, String[]> params, Annotation[] annotations) {
		try {
			BeanWrapper bw = new BeanWrapper(o.getClass());
			// Start with relations
			Set<Field> fields = new HashSet<Field>();
			Class<?> clazz = o.getClass();
			while (!clazz.equals(Model.class)) {
				Collections.addAll(fields, clazz.getDeclaredFields());
				clazz = clazz.getSuperclass();
			}
			for (Field field : fields) {
				boolean isEntity = false;
				boolean isJson = false;
				String relation = null;
				boolean multiple = false;
				String owner = null;

				// ONE TO MANY or ONE TO ONE association
				// entity = type inherits SienaSupport
				if(Model.class.isAssignableFrom(field.getType())) {
					isEntity = true;
					relation = field.getType().getName();
				}

				// MANY TO ONE association
				// type QUERY<T> + annotation @Filter 
				else if(siena.Query.class.isAssignableFrom(field.getType())){
					isEntity = true;
					multiple = true;
					Class<?> fieldType = 
						(Class<?>) ((ParameterizedType) 
								field.getGenericType()).getActualTypeArguments()[0];
					relation = fieldType.getName();
					owner = field.getAnnotation(Filter.class).value();
					// by default, takes the type of the parent entity in lower case
					if(owner == null || "".equals(owner)){
						owner = o.getClass().getName().toLowerCase();
					}
				}
				else if(Json.class.isAssignableFrom(field.getType())){
					isJson = true;
				}
				else if(field.isAnnotationPresent(Embedded.class)){
					if(List.class.isAssignableFrom(field.getType())){
						multiple = true;
	            	}
					else if(Map.class.isAssignableFrom(field.getType())){
						multiple = true;
	            	}
	            	else {
	            		multiple = false;
	            	}
				}
				else if(byte[].class.isAssignableFrom(field.getType())
						/*|| Blob.class.isAssignableFrom(field.getType())*/)
				{
					// if params is present but empty, resets the older value
					String[] posted = params.get(name + "." + field.getName());
					// TODO
					Object val = field.get(o);	
					//params.put(name + "." + field.getName(), val);
				}
				
				if (isEntity) {
					// builds entity list for many to one
					if (multiple) {
						//Collection l = new ArrayList();

						String[] ids = params.get(name + "." + field.getName() + "@id");
						if(ids == null) {
							ids = params.get(name + "." + field.getName() + ".id");
						}

						if (ids != null) {
							params.remove(name + "." + field.getName() + ".id");
							params.remove(name + "." + field.getName() + "@id");
							for (String _id : ids) {
								if (_id.equals("")) {
									continue;
								}
								@SuppressWarnings("unchecked")
								Class<? extends Model> relClass = (Class<? extends Model>)Play.classloader.loadClass(relation);
								Model res = 
									Model.all(relClass)
										.filter("id", Binder.directBind(_id, Model.Manager.factoryFor(relClass).keyType()))
										.get();
								if(res!=null){
									// sets the object to the owner field into the relation entity
									relClass.getField(owner).set(res, o);
									res.save();
								}
									
								else Validation.addError(name+"."+field.getName(), "validation.notFound", _id);
							}
							// can't set arraylist to Query<T>
							// bw.set(field.getName(), o, l);
						}
					}
					// builds simple entity for simple association
					else {
						String[] ids = params.get(name + "." + field.getName() + "@id");
						if(ids == null) {
							ids = params.get(name + "." + field.getName() + ".id");
						}
						if (ids != null && ids.length > 0 && !ids[0].equals("")) {
							params.remove(name + "." + field.getName() + ".id");
							params.remove(name + "." + field.getName() + "@id");

							@SuppressWarnings("unchecked")
							Class<? extends Model> relClass = (Class<? extends Model>)Play.classloader.loadClass(relation);
							Object res = 
								Model.all(relClass)
									.filter("id", Binder.directBind(ids[0], Model.Manager.factoryFor(relClass).keyType()))
									.get();
							if(res!=null)
								bw.set(field.getName(), o, res);
							else Validation.addError(name+"."+field.getName(), "validation.notFound", ids[0]);

						} else if(ids != null && ids.length > 0 && ids[0].equals("")) {
							bw.set(field.getName(), o , null);
							params.remove(name + "." + field.getName() + ".id");
							params.remove(name + "." + field.getName() + "@id");
						}
					}	                	
				}
				else if(isJson){
					String[] jsonStr = params.get(name + "." + field.getName());
					if (jsonStr != null && jsonStr.length > 0 && !jsonStr[0].equals("")) {
						try {
							//com.google.gson.JsonParser parser = new com.google.gson.JsonParser();
							//parser.parse(jsonStr[0]);
							
							Json json = Json.loads(jsonStr[0]);
							if(json!=null){
								bw.set(field.getName(), o, json);
								params.remove(name + "." + field.getName());
							}
							else Validation.addError(name+"."+field.getName(), "validation.notParsable");
						}catch(JsonParseException ex){
							ex.printStackTrace();
							Logger.error("json parserdelete exception:%s", 
									ex.getCause()!=null?ex.getCause().getMessage(): ex.getMessage());
							Validation.addError(
									name+"."+field.getName(), 
									"validation.notParsable", 
									ex.getCause()!=null?ex.getCause().getMessage(): ex.getMessage());
						}catch(SienaException ex){
							ex.printStackTrace();
							Logger.error("json parserdelete exception:%s", 
									ex.getCause()!=null?ex.getCause().getMessage(): ex.getMessage());
							Validation.addError(
									name+"."+field.getName(), 
									"validation.notParsable", 
									ex.getCause()!=null?ex.getCause().getMessage(): ex.getMessage());
						}
						catch(IllegalArgumentException ex){
							ex.printStackTrace();
							Logger.error("json parser exception:%s", 
									ex.getCause()!=null?ex.getCause().getMessage(): ex.getMessage());
							Validation.addError(
									name+"."+field.getName(), 
									"validation.notParsable", 
									ex.getCause()!=null?ex.getCause().getMessage(): ex.getMessage());
						}
					}
				}	
			}
			// Then bind
			// all composites objects (simple entity, list and maps) are managed
			// by this function
			// v1.0.x code
			// bw.bind(name, o.getClass(), params, "", o);

			// v1.1 compliant
			bw.bind(name, (Type)o.getClass(), params, "", o, o.getClass().getAnnotations());
			
			return (T) o;
		} catch (Exception e) {
			throw new UnexpectedException(e);
		}
	}

	// validates and inserts the entity
    public boolean validateAndSave() {
        if(Validation.current().valid(this).ok) {
            this.insert();
            return true;
        }
        return false;
    }
    
    // functions to enhance    
    // we don't return a siena.Query because the strict generic typed siena.Query gives compilation
    // errors when you create a class inheriting from Model and calling all().filter().fetch() for ex
	public static Query all() {
    	throw new UnsupportedOperationException(
              "Please annotate your model with @siena.Entity annotation.");
     }

    public static <T extends Model> Batch<T> batch() {
    	throw new UnsupportedOperationException(
    		"Please annotate your model with @siena.Entity annotation.");
     }
    
     public static <T extends Model> T create(String name, Params params) {
    	 throw new UnsupportedOperationException(
    	 	"Please annotate your model with @siena.Entity annotation.");
     }
     
     public static long count() {
    	 throw new UnsupportedOperationException(
 	 		"Please annotate your model with @siena.Entity annotation.");
     }
     
     public static <T extends Model> List<T> findAll() {
    	 throw new UnsupportedOperationException(
    	 	"Please annotate your model with @siena.Entity annotation.");
      }
     
     public static long deleteAll() {
    	 throw new UnsupportedOperationException(
    	 	"Please annotate your model with @siena.Entity annotation.");
     }
     
     public static <T extends Model> T findById(Object id) {
    	 throw new UnsupportedOperationException(
    	 	"Please annotate your model with @siena.Entity annotation.");
     }
}

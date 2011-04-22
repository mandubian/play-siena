/**
 * 
 */
package play.modules.siena;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
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
import siena.Query;
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
		this.insert();
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

	@SuppressWarnings("unchecked")
	public static <T extends Model> T create(Class<?> type, String name,
			Map<String, String[]> params, Annotation[] annotations) {
		try {
			Constructor<?> c = type.getDeclaredConstructor();
			c.setAccessible(true);
			Object model = c.newInstance();
			return (T) edit(model, name, params, annotations);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public static <T extends Model> T edit(Object o, String name, Map<String, String[]> params, Annotation[] annotations) {
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

				// ONE TO MANY association
				// entity = type inherits SienaSupport
				if(Model.class.isAssignableFrom(field.getType())) {
					isEntity = true;
					relation = field.getType().getName();
				}

				// MANY TO ONE association
				// type QUERY<T> + annotation @Filter 
				else if(Query.class.isAssignableFrom(field.getType())){
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
								Object res = 
									Model.all(relClass)
										.filter("id", Binder.directBind(_id, Model.Manager.factoryFor(relClass).keyType()))
										.get();
								if(res!=null){
									// sets the object to the owner field into the relation entity
									relClass.getField(owner).set(res, o);
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
							com.google.gson.JsonParser parser = new com.google.gson.JsonParser();
							parser.parse(jsonStr[0]);
							
							params.remove(name + "." + field.getName());
							Json json = Json.loads(jsonStr[0]);
							if(json!=null)
								bw.set(field.getName(), o, json);
							else Validation.addError(name+"."+field.getName(), "validation.notParsable");
						}catch(JsonParseException ex){
							ex.printStackTrace();
							Logger.error("json parser exception:%s", 
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
    public static <T extends Model> Query<T> all() {
    	throw new UnsupportedOperationException(
              "Please extends your model from @play.modules.siena.Model to be enhanced.");
     }

    public static <T extends Model> Batch<T> batch() {
    	throw new UnsupportedOperationException(
        	"Please extends your model from @play.modules.siena.Model to be enhanced.");
     }
    
     public static <T extends Model> T create(String name, Params params) {
    	 throw new UnsupportedOperationException(
        	"Please extends your model from @play.modules.siena.Model to be enhanced.");
     }
     
     public static long count() {
    	 throw new UnsupportedOperationException(
     		"Please extends your model from @play.modules.siena.Model to be enhanced.");
     }
     
     public static <T extends Model> List<T> findAll() {
         throw new UnsupportedOperationException(
         	"Please extends your model from @play.modules.siena.Model to be enhanced.");
      }
     
     public static long deleteAll() {
    	 throw new UnsupportedOperationException(
    	 	"Please extends your model from @play.modules.siena.Model to be enhanced.");
     }
     
     public static <T extends Model> T findById(Object id) {
    	 throw new UnsupportedOperationException(
    	 	"Please extends your model from @play.modules.siena.Model to be enhanced.");
     }
}

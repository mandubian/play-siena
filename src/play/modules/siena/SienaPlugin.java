package play.modules.siena;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import play.Logger;
import play.Play;
import play.PlayPlugin;
import play.data.binding.Binder;
import play.db.Model.Property;
import play.db.jpa.JPA;
import play.exceptions.UnexpectedException;
import siena.ClassInfo;
import siena.Json;
import siena.PersistenceManager;
import siena.PersistenceManagerFactory;
import siena.Query;
import siena.gae.GaePersistenceManager;
import siena.jdbc.JdbcPersistenceManager;
import siena.jdbc.PostgresqlPersistenceManager;
import siena.jdbc.ddl.DdlGenerator;

public class SienaPlugin extends PlayPlugin {
    
    private static PersistenceManager persistenceManager;
    private static DdlGenerator generator;
    
    public static PersistenceManager pm() {
        return persistenceManager;
    }
    
    @Override
    public void onApplicationStart() {
    	// DISABLES JPA
    	if(JPA.isEnabled()){
    		Play.pluginCollection.disablePlugin(play.db.jpa.JPAPlugin.class);
    	}
        // GAE ?
        boolean gae = false;       
        for(PlayPlugin plugin : Play.pluginCollection.getEnabledPlugins()) {
            if(plugin.getClass().getSimpleName().equals("GAEPlugin")) {
                gae = true;
                break;
            }
        }
        
        // The persistence manager
        if(!gae) {
        	// initializes DDL Generator
        	generator = new DdlGenerator();
        	
        	// determines if it is Postgres
        	final String db = Play.configuration.getProperty("db");
            final String dbUrl = Play.configuration.getProperty("db.url");
            if((db==null || db=="" ) && (dbUrl == null || dbUrl == "")){
            	throw new UnexpectedException("SienaPlugin : not using GAE requires at least a db config");
            }
            if((db!=null && db.contains("postgres")) || (dbUrl!=null && dbUrl.contains("postgres"))){
            	persistenceManager = new PostgresqlPersistenceManager(new PlayConnectionManager(), null);
            }
            else {
            	persistenceManager = new JdbcPersistenceManager(new PlayConnectionManager(), null);
            }
        } else {
            persistenceManager = new GaePersistenceManager();
            persistenceManager.init(null);
        }
        
        // Install all classes
        for(Class<?> c : Play.classloader.getAssignableClasses(Model.class)) {
        	// adds classes to the DDL generator
        	generator.addTable(c);
        	// installs it into the PM
            PersistenceManagerFactory.install(persistenceManager, c);
        }
    }
    
    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Object bind(String name, Class clazz, java.lang.reflect.Type type, Annotation[] annotations, Map<String, String[]> params) 
    {
        // TODO need to be more generic in order to work with JPASupport
        if (Model.class.isAssignableFrom(clazz)) {
            String keyName = Model.Manager.factoryFor(clazz).keyName();
            String idKey = name + "." + keyName;
            if (params.containsKey(idKey) && params.get(idKey).length > 0 && params.get(idKey)[0] != null && params.get(idKey)[0].trim().length() > 0) {
                String id = params.get(idKey)[0];
                try {
                    Query<?> query = pm().createQuery(clazz).filter(keyName, 
                    		play.data.binding.Binder.directBind(name, annotations, id + "", Model.Manager.factoryFor(clazz).keyType()));
                    Object o = query.get();
                    return Model.edit(o, name, params, annotations);
                } catch (Exception e) {
                    throw new UnexpectedException(e);
                }
            }
            return Model.create(clazz, name, params, annotations);
        }
        return super.bind(name, clazz, type, annotations, params);
    }

    @Override
    public Object bind(String name, Object o, Map<String, String[]> params) {
        if (o instanceof Model) {
            return Model.edit(o, name, params, null);
        }
        return null;
    }
    
    @SuppressWarnings("unchecked")
	@Override
    public play.db.Model.Factory modelFactory(Class<? extends play.db.Model> modelClass) {
        if (Model.class.isAssignableFrom(modelClass)) {
            return new SienaModelLoader((Class<? extends Model>)modelClass);
        }
        return null;
    }
    
    public static class SienaModelLoader implements play.db.Model.Factory {
    	private Class<? extends Model> clazz;
    	private ClassInfo sienaInfo;
    	
    	public SienaModelLoader(Class<? extends Model> clazz) {
            this.clazz = clazz;
            this.sienaInfo = ClassInfo.getClassInfo(clazz);
        }
    	
    	@Override
		public play.db.Model findById(Object id) {
    		if (id == null) {
                return null;
            }
    		try {
                return pm().getByKeys(clazz, Binder.directBind(id.toString(), keyType())).get(0);
            } catch (Exception e) {
                // Key is invalid, thus nothing was found
                return null;
            }
		}
    	
        @Override
        public String keyName() {
            Field f = keyField();
            return (f == null) ? null : f.getName();
        }

        @Override
        public Class<?> keyType() {
            return keyField().getType();
        }

        @Override
        public Object keyValue(play.db.Model m) {
            Field k = keyField();
            try {
                // Embedded class has no key value
                return null != k ? k.get(m) : null;
            } catch (Exception ex) {
                throw new UnexpectedException(ex);
            }
        }

        //
        Field keyField() {
            return sienaInfo.getIdField();
        }

        /*
         * code directly inspired from Morphia Play Plugin 
         * https://github.com/greenlaw110/play-morphia
         * 
         * Support the following syntax at the moment: property = 'val' property
         * in ('val1', 'val2' ...) prop1 ... and prop2 ...
         */
        private static void processWhere(Query<?> q, String where) {
            if (null != where) {
                where = where.trim();
            } else {
                where = "";
            }
            if ("".equals(where) || "null".equalsIgnoreCase(where))
                return;
            
            String[] propValPairs = where.split("(and|&&)");
            for (String propVal : propValPairs) {
                if (propVal.contains("=")) {
                    String[] sa = propVal.split("=");
                    if (sa.length != 2) {
                        throw new IllegalArgumentException(
                                "invalid where clause: " + where);
                    }
                    String prop = sa[0];
                    String val = sa[1];
                    Logger.trace("where prop val pair found: %1$s = %2$s",
                            prop, val);
                    prop = prop.replaceAll("[\"' ]", "");
                    if (val.matches("[\"'].*[\"']")) {
                        // string value
                        val = val.replaceAll("[\"' ]", "");
                        q.filter(prop, val);
                    } else {
                        // possible string, number or boolean value
                        if (val.matches("[-+]?\\d+\\.\\d+")) {
                            q.filter(prop, Float.parseFloat(val));
                        } else if (val.matches("[-+]?\\d+")) {
                            q.filter(prop, Integer.parseInt(val));
                        } else if (val
                                .matches("(false|true|FALSE|TRUE|False|True)")) {
                            q.filter(prop, Boolean.parseBoolean(val));
                        } else {
                            q.filter(prop, val);
                        }
                    }
                } else if (propVal.contains(" in ")) {
                    String[] sa = propVal.split(" in ");
                    if (sa.length != 2) {
                        throw new IllegalArgumentException(
                                "invalid where clause: " + where);
                    }
                    String prop = sa[0].trim();
                    String val0 = sa[1].trim();
                    if (!val0.matches("\\(.*\\)")) {
                        throw new IllegalArgumentException(
                                "invalid where clause: " + where);
                    }
                    val0 = val0.replaceAll("[\\(\\)]", "");
                    String[] vals = val0.split(",");
                    List<Object> l = new ArrayList<Object>();
                    for (String val : vals) {
                        // possible string, number or boolean value
                        if (val.matches("[-+]?\\d+\\.\\d+")) {
                            l.add(Float.parseFloat(val));
                        } else if (val.matches("[-+]?\\d+")) {
                            l.add(Integer.parseInt(val));
                        } else if (val
                                .matches("(false|true|FALSE|TRUE|False|True)")) {
                            l.add(Boolean.parseBoolean(val));
                        } else {
                            l.add(val);
                        }
                    }
                    q.filter(prop + " IN ", l);
                } else {
                    throw new IllegalArgumentException("invalid where clause: "
                            + where);
                }
            }
        }
        
		@SuppressWarnings("unchecked")
		@Override
		public List<play.db.Model> fetch(int offset, int size, 
				String orderBy,	String order, 
				List<String> searchFields, String keywords, String where) {
			Query<?> q = pm().createQuery(clazz);
			
			// ORDER
			if(orderBy == null) {
				if (order == null) {
					q.order(keyField().getName());
				}
				else {
					if(order.equals("+") || order.equals("-")){
						q.order(order+keyField().getName());
					}
					else if(order.equals("ASC")){
						q.order("+"+keyField().getName());
					}	
					else if(order.equals("DESC")){
						q.order("-"+keyField().getName());
					}
					else {
						q.order(keyField().getName());
					} 
				}
			}
			else {
				if (order == null) {
					q.order(orderBy);
				}
				else {
					if(order.equals("+") || order.equals("-")){
						q.order(order+orderBy);
					}
					else if(order.equals("ASC")){
						q.order("+"+orderBy);
					}
					else if(order.equals("DESC")){
						q.order("-"+orderBy);
					}
					else {
						q.order(orderBy);
					} 
				}
			}
			
			// SEARCH
			// TODO define the search strings
			if(keywords != null && searchFields != null && searchFields.size() != 0){
				q.search(keywords, (String[])searchFields.toArray());
			}
			
			// WHERE
			processWhere(q, where);
			
			return (List<play.db.Model>)q.fetch(size, offset);
		}

		@Override
		public Long count(List<String> searchFields, String keywords, String where) {
			Query<?> q = pm().createQuery(clazz);
			
			// SEARCH
			// TODO define the search strings
			if(keywords != null && searchFields != null && searchFields.size() != 0){
				q.search(keywords, (String[])searchFields.toArray());
			}
			
			// WHERE
			processWhere(q, where);
			
			return new Long(q.count());
		}

		@Override
		public void deleteAll() {
			pm().createQuery(clazz).delete();
		}

		@Override
		public List<Property> listProperties() {
			List<Model.Property> properties = new ArrayList<Model.Property>();
            Set<Field> fields = new LinkedHashSet<Field>();
            Collections.addAll(fields, (Field[])ClassInfo.getClassInfo(clazz).allFields.toArray());

            for (Field f : fields) {
                Model.Property mp = buildProperty(f);
                if (mp != null) {
                    properties.add(mp);
                }
            }
            return properties;
		}
		
        private Model.Property buildProperty(final Field field) {
            Model.Property modelProperty = new Model.Property();
            modelProperty.type = field.getType();
            modelProperty.field = field;
            // ONE-TO-ONE / MANY-TO-ONE
            if (Model.class.isAssignableFrom(field.getType())) {
            	modelProperty.isRelation = true;
                modelProperty.relationType = field.getType();
                modelProperty.choices = new Model.Choices() {

                    @SuppressWarnings("unchecked")
                    public List<Object> list() {
                    	return (List<Object>)pm().createQuery(field.getType()).fetch();
                    }
                };
            }
            // AUTOMATIC QUERY
            // ONE-TO-MANY
            if (Query.class.isAssignableFrom(field.getType())) {
                final Class<?> fieldType = (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
                
                modelProperty.isRelation = true;
                modelProperty.isMultiple = true;
                modelProperty.relationType = fieldType;
                modelProperty.choices = new Model.Choices() {
                	@SuppressWarnings("unchecked")
                	public List<Object> list() {
                		return (List<Object>)pm().createQuery(fieldType).fetch();
                	}
                };
            }
            
            // ENUM
            if (field.getType().isEnum()) {
                modelProperty.choices = new Model.Choices() {
                    @SuppressWarnings("unchecked")
                    public List<Object> list() {
                        return (List<Object>) Arrays.asList(field.getType().getEnumConstants());
                    }
                };
            }
            
            // JSON
            if (Json.class.isAssignableFrom(field.getType())) {
                modelProperty.choices = new Model.Choices() {
                    @SuppressWarnings("unchecked")
                    public List<Object> list() {
                        return (List<Object>) Arrays.asList(field.getType().getEnumConstants());
                    }
                };
            }

            modelProperty.name = field.getName();
            if (field.getType().equals(String.class)) {
                modelProperty.isSearchable = true;
            }
            if(sienaInfo.generatedKeys.contains(field)){
            	modelProperty.isGenerated = true;
            }
            return modelProperty;
        }
    }
}

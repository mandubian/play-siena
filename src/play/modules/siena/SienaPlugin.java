package play.modules.siena;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ddlutils.DatabaseOperationException;
import org.apache.ddlutils.Platform;
import org.apache.ddlutils.PlatformFactory;
import org.apache.ddlutils.model.Database;

import play.Logger;
import play.Play;
import play.PlayPlugin;
import play.classloading.ApplicationClasses.ApplicationClass;
import play.data.binding.Binder;
import play.db.Model.Property;
import play.db.jpa.JPA;
import play.exceptions.UnexpectedException;
import siena.ClassInfo;
import siena.Generator;
import siena.Id;
import siena.Json;
import siena.PersistenceManager;
import siena.PersistenceManagerFactory;
import siena.Query;
import siena.embed.Embedded;
import siena.gae.GaePersistenceManager;
import siena.jdbc.H2PersistenceManager;
import siena.jdbc.JdbcPersistenceManager;
import siena.jdbc.PostgresqlPersistenceManager;
import siena.jdbc.ddl.DdlGenerator;

public class SienaPlugin extends PlayPlugin {
    
    private static PersistenceManager persistenceManager;
    private static DdlGenerator generator;
    
    private SienaEnhancer enhancer = new SienaEnhancer();

    public static PersistenceManager pm() {
        return persistenceManager;
    }
    
    public static String dbType(){
    	for(PlayPlugin plugin : Play.pluginCollection.getEnabledPlugins()) {
            if(plugin.getClass().getSimpleName().equals("GAEPlugin")) {
                return "nosql:gae";
            }
        }
    	
    	final String db = Play.configuration.getProperty("db");
        final String dbUrl = Play.configuration.getProperty("db.url");
        if((db==null || db=="" ) && (dbUrl == null || dbUrl == "")){
        	throw new UnexpectedException("SienaPlugin : not using GAE requires at least a db config");
        }
        if((db!=null && db.contains("postgres")) || (dbUrl!=null && dbUrl.contains("postgres"))){
        	return "sql:postgresql";
        }else if((db!=null && db.contains("h2")) || (dbUrl!=null && dbUrl.contains("h2"))){
        	return "sql:h2:mysql";
        }else {
        	return "sql:mysql";
        }
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

        @SuppressWarnings("rawtypes")
		List<Class> classes = Play.classloader.getAssignableClasses(Model.class);
        
        // DDL is for SQL and not in prod mode
        if(!gae) {        	
        	// JDBC
        	String ddlType = "mysql";
        	// initializes DDL Generator
        	generator = new DdlGenerator();
			Connection connection = new PlayConnectionManager().getConnection();

			// determines DB type
			final String dbType = dbType();
			Logger.debug("Siena DB Type: %s", dbType);
			final String db = Play.configuration.getProperty("db");
            final String dbUrl = Play.configuration.getProperty("db.url");
            if((db==null || db=="" ) && (dbUrl == null || dbUrl == "")){
            	throw new UnexpectedException("SienaPlugin : not using GAE requires at least a db config");
            }
            if(dbType.contains("postgresql")){
            	persistenceManager = new PostgresqlPersistenceManager(new PlayConnectionManager(), null);
            	ddlType = "postgresql";
            }else if(dbType.contains("h2")){
            	// the H2 dbMode in Play is "mysql" 
            	persistenceManager = new H2PersistenceManager(new PlayConnectionManager(), null, "mysql");
            	ddlType = "mysql";
            }
            else {
            	persistenceManager = new JdbcPersistenceManager(new PlayConnectionManager(), null);
            }
            
            // Alter tables before installing
            for(Class<?> c : classes) {
            	// adds classes to the DDL generator
            	generator.addTable(c);
            }
            // get the Database model
			Database database = generator.getDatabase();
	
			Platform platform = PlatformFactory.createNewPlatformInstance(ddlType);
						
			// siena.ddl can have create/update/ddl
			// if siena.ddl is defined, uses it
			// if not: 
			// in dev mode, will be update by default
			// in prod mode, will be none by default
			if(Play.mode.isDev()){
				String ddl = Play.configuration.getProperty("siena.ddl", "update");
				Logger.debug("Siena DDL dev mode: %s", ddl);
				if ("create".equals(ddl)) {
					Logger.debug("Siena DDL Generator SQL: %s", platform.getAlterTablesSql(connection, database));
					// creates tables and do not drop tables and do not continues on error 
					try {
						platform.createTables(connection, database, false, false);
					}catch(DatabaseOperationException ex){
						Logger.warn("Siena DDL createTables generated exception:%s", ex.getCause()!=null?ex.getCause():ex.getMessage());
					}
				}else if("update".equals(ddl)){
					Logger.debug("Siena DDL Generator SQL: %s", platform.getAlterTablesSql(connection, database));
					// alters tables and continues on error 
					platform.alterTables(connection, database, true);
				}
			}
			else if(Play.mode.isProd()){
				String ddl = Play.configuration.getProperty("siena.ddl", "none");
				Logger.debug("Siena DDL prod mode: %s", ddl);
				if ("create".equals(ddl)) {
					Logger.debug("Siena DDL Generator SQL: %s", platform.getAlterTablesSql(connection, database));
					// creates tables and do not drop tables and do not continues on error 
					try {
						platform.createTables(connection, database, false, false);
					}catch(DatabaseOperationException ex){
						Logger.warn("Siena DDL createTables generated exception:%s", ex.getCause()!=null?ex.getCause():ex.getMessage());
					}
				}else if("update".equals(ddl)){
					Logger.debug("Siena DDL Generator SQL: %s", platform.getAlterTablesSql(connection, database));
					// alters tables and continues on error 
					platform.alterTables(connection, database, true);
				}
			}
			
			// is it required ?
			// connection.close();
            persistenceManager.init(null);
			                    
        } else {
			Logger.debug("Siena DB Type: GAE");
            persistenceManager = new GaePersistenceManager();
            persistenceManager.init(null);
        }

        // Install all classes in PersistenceManager
        for(Class<?> c : classes) {
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
            	ClassInfo sienaInfo = ClassInfo.getClassInfo(clazz);
            	Field idField = sienaInfo.getIdField();
            	Id idAnn = idField.getAnnotation(Id.class);
    			if(idAnn != null && idAnn.value() == Generator.AUTO_INCREMENT) {
    				// ONLY long ID can be auto_incremented
	                String id = params.get(idKey)[0];
	                try {
	                    Query<? extends Model> query = pm().createQuery(clazz).filter(keyName, 
	                    		play.data.binding.Binder.directBind(name, annotations, id + "", Model.Manager.factoryFor(clazz).keyType()));
	                    Model o = query.get();
	                    return Model.edit(o, name, params, annotations);
	                } catch (Exception e) {
	                    throw new UnexpectedException(e);
	                }
    			}
            }
            return Model.create(clazz, name, params, annotations);
        }
        return super.bind(name, clazz, type, annotations, params);
    }

    @Override
    public Object bind(String name, Object o, Map<String, String[]> params) {
        if (o instanceof Model) {
            return Model.edit((Model)o, name, params, null);
        }
        return null;
    }
    
    @Override
    public void enhance(ApplicationClass applicationClass) throws Exception {
        enhancer.enhanceThisClass(applicationClass);
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
			if(keywords != null){
				if(searchFields != null && searchFields.size() != 0){
					q.search(keywords, (String[])searchFields.toArray());
				}else{
					ClassInfo ci = ClassInfo.getClassInfo(clazz);
					String[] strs = new String[ci.allFields.size()];
					int i=0;
					for(Field f : ClassInfo.getClassInfo(clazz).allFields){
						strs[i++] = f.getName();
					}
					q.search(keywords, strs);
				}
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
			if(keywords != null){
				if(searchFields != null && searchFields.size() != 0){
					q.search(keywords, (String[])searchFields.toArray());
				}else{
					ClassInfo ci = ClassInfo.getClassInfo(clazz);
					String[] strs = new String[ci.allFields.size()];
					int i=0;
					for(Field f : ClassInfo.getClassInfo(clazz).allFields){
						strs[i++] = f.getName();
					}
					q.search(keywords, strs);
				}
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
            // can't use classInfo.allFields as we need also Query fields
            for(Field f:clazz.getDeclaredFields()){
            	if(f.getType() == Class.class ||
            			(f.getModifiers() & Modifier.TRANSIENT) == Modifier.TRANSIENT ||
            			(f.getModifiers() & Modifier.STATIC) == Modifier.STATIC ||
            			f.isSynthetic()) 
            	{         
            		continue;
            	}
            	fields.add(f);
            }

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
                modelProperty.type = String.class;
            }

            if (field.isAnnotationPresent(Embedded.class)) {
            	if(List.class.isAssignableFrom(field.getType())){
            		final Class<?> fieldType = (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
            		
            		modelProperty.isRelation = true;
                    modelProperty.isMultiple = true;
                    modelProperty.relationType = fieldType;
            	}
            	else if(Map.class.isAssignableFrom(field.getType())){
            		// gets T2 for map<T1,T2>
            		final Class<?> fieldType = (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[1];
            		modelProperty.isRelation = true;
                    modelProperty.isMultiple = true;
                    modelProperty.relationType = fieldType;
            	}
            	else {
            		modelProperty.isRelation = true;
            		modelProperty.isMultiple = false;
            		modelProperty.relationType = field.getType();
            	}
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

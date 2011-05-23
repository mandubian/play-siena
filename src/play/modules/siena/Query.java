package play.modules.siena;

import java.util.List;
import java.util.Map;

import siena.PersistenceManager;
import siena.QueryFilter;
import siena.QueryFilterSearch;
import siena.QueryJoin;
import siena.QueryOrder;
import siena.core.options.QueryOption;

public class Query{
	@SuppressWarnings("rawtypes")
	siena.Query query;
	
	@SuppressWarnings("rawtypes")
	public <T> Query(siena.Query query){
		this.query = query;
	}
	
	@SuppressWarnings("unchecked")
	public <T> Class<T> getQueriedClass(){
		return (Class<T>)query.getQueriedClass();		
	}
	
	@SuppressWarnings("unchecked")
	public List<QueryFilter> getFilters() {
		return query.getFilters();
	}

	@SuppressWarnings("unchecked")
	public List<QueryOrder> getOrders() {
		return query.getOrders();
	}

	@SuppressWarnings("unchecked")
	public List<QueryFilterSearch> getSearches() {
		return query.getSearches();
	}

	@SuppressWarnings("unchecked")
	public List<QueryJoin> getJoins() {
		return query.getJoins();
	}
	
	public QueryOption option(int option) {
		return query.option(option);
	}
	
	@SuppressWarnings("unchecked")
	public Map<Integer, QueryOption> options() {
		return query.options();
	}

	public <T> Query filter(String fieldName, Object value) {
		query.filter(fieldName, value);
		return this;
	}

	public <T> Query order(String fieldName) {
		query.order(fieldName);
		return this;
	}

	public <T> Query join(String field, String... sortFields) {
		query.join(field, sortFields);
		return this;
	}

	public <T> Query search(String match, String... fields) {
		query.search(match, fields);
		return this;
	}

	public <T> Query search(String match, QueryOption opt, String... fields) {
		query.search(match, opt, fields);
		return this;
	}

	@SuppressWarnings("unchecked")
	public <T> T get() {
		return (T)query.get();
	}

	public <T> int delete() {
		return query.delete();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public <T> int update(Map fieldValues) {
		return query.update(fieldValues);
	}

	public <T> int count() {
		return query.count();
	}

	@SuppressWarnings("unchecked")
	public <T> T getByKey(Object key) {
		return (T)query.getByKey(key);
	}

	@SuppressWarnings("unchecked")
	public <T> List<T> fetch() {
		return (List<T>)query.fetch();
	}

	@SuppressWarnings("unchecked")
	public <T> List<T> fetch(int limit) {
		return (List<T>)query.fetch(limit);
	}

	@SuppressWarnings("unchecked")
	public <T> List<T> fetch(int limit, Object offset) {
		return (List<T>)query.fetch(limit, offset);
	}

	@SuppressWarnings("unchecked")
	public <T> List<T> fetchKeys() {
		return (List<T>)query.fetchKeys();
	}

	@SuppressWarnings("unchecked")
	public <T> List<T> fetchKeys(int limit) {
		return (List<T>)query.fetchKeys(limit);
	}

	@SuppressWarnings("unchecked")
	public <T> List<T> fetchKeys(int limit, Object offset) {
		return (List<T>)query.fetchKeys(limit, offset);
	}

	@SuppressWarnings("unchecked")
	public <T> Iterable<T> iter() {
		return (Iterable<T>)query.iter();
	}

	@SuppressWarnings("unchecked")
	public <T> Iterable<T> iter(int limit) {
		return (Iterable<T>)query.iter(limit);
	}

	@SuppressWarnings("unchecked")
	public <T> Iterable<T> iter(int limit, Object offset) {
		return (Iterable<T>)query.iter(limit, offset);
	}

	@SuppressWarnings("unchecked")
	public <T> Iterable<T> iterPerPage(int limit) {
		return query.iterPerPage(limit);
	}

	public <T> Query limit(int limit) {
		query.limit(limit);
		return this;
	}

	public <T> Query offset(Object offset) {
		query.offset(offset);
		return this;
	}

	public <T> Query paginate(int size) {
		query.paginate(size);
		return this;
	}

	public <T> Query nextPage() {
		query.nextPage();
		return this;
	}

	public <T> Query previousPage() {
		query.previousPage();
		return this;
	}

	public <T> Query customize(QueryOption... options) {
		query.customize(options);
		return this;
	}

	public <T> Query stateful() {
		query.stateful();
		return this;
	}

	public <T> Query stateless() {
		query.stateless();
		return this;
	}

	public <T> Query release() {
		query.release();
		return this;
	}

	public <T> Query resetData() {
		query.resetData();
		return this;
	}

	public <T> Query dump() {
		query.dump();
		return this;
	}

	public <T> Query restore(String dump) {
		query.restore(dump);
		return this;
	}

	public <T> QueryAsync async() {
		return new QueryAsync(query.async());
	}

	public PersistenceManager getPersistenceManager() {
		return query.getPersistenceManager();
	}


}

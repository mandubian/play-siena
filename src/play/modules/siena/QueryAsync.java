package play.modules.siena;

import java.util.List;
import java.util.Map;

import siena.QueryFilter;
import siena.QueryFilterSearch;
import siena.QueryJoin;
import siena.QueryOrder;
import siena.core.async.PersistenceManagerAsync;
import siena.core.async.SienaFuture;
import siena.core.options.QueryOption;

public class QueryAsync{
	@SuppressWarnings("rawtypes")
	siena.core.async.QueryAsync query;
	
	@SuppressWarnings("rawtypes")
	public <T> QueryAsync(siena.core.async.QueryAsync query){
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

	public <T> QueryAsync filter(String fieldName, Object value) {
		query.filter(fieldName, value);
		return this;
	}

	public <T> QueryAsync order(String fieldName) {
		query.order(fieldName);
		return this;
	}

	public <T> QueryAsync join(String field, String... sortFields) {
		query.join(field, sortFields);
		return this;
	}

	public <T> QueryAsync search(String match, String... fields) {
		query.search(match, fields);
		return this;
	}

	public <T> QueryAsync search(String match, QueryOption opt, String... fields) {
		query.search(match, opt, fields);
		return this;
	}

	@SuppressWarnings("unchecked")
	public <T> SienaFuture<T> get() {
		return (SienaFuture<T>)query.get();
	}

	@SuppressWarnings("unchecked")
	public <T> SienaFuture<Integer> delete() {
		return query.delete();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public <T> SienaFuture<Integer> update(Map fieldValues) {
		return query.update(fieldValues);
	}

	@SuppressWarnings("unchecked")
	public <T> SienaFuture<Integer> count() {
		return query.count();
	}

	@SuppressWarnings("unchecked")
	public <T> SienaFuture<T> getByKey(Object key) {
		return (SienaFuture<T>)query.getByKey(key);
	}

	@SuppressWarnings("unchecked")
	public <T> SienaFuture<List<T>> fetch() {
		return (SienaFuture<List<T>>)query.fetch();
	}

	@SuppressWarnings("unchecked")
	public <T> SienaFuture<List<T>> fetch(int limit) {
		return (SienaFuture<List<T>>)query.fetch(limit);
	}

	@SuppressWarnings("unchecked")
	public <T> SienaFuture<List<T>> fetch(int limit, Object offset) {
		return (SienaFuture<List<T>>)query.fetch(limit, offset);
	}

	@SuppressWarnings("unchecked")
	public <T> SienaFuture<List<T>> fetchKeys() {
		return (SienaFuture<List<T>>)query.fetchKeys();
	}

	@SuppressWarnings("unchecked")
	public <T> SienaFuture<List<T>> fetchKeys(int limit) {
		return (SienaFuture<List<T>>)query.fetchKeys(limit);
	}

	@SuppressWarnings("unchecked")
	public <T> SienaFuture<List<T>> fetchKeys(int limit, Object offset) {
		return (SienaFuture<List<T>>)query.fetchKeys(limit, offset);
	}

	@SuppressWarnings("unchecked")
	public <T> SienaFuture<Iterable<T>> iter() {
		return (SienaFuture<Iterable<T>>)query.iter();
	}

	@SuppressWarnings("unchecked")
	public <T> SienaFuture<Iterable<T>> iter(int limit) {
		return (SienaFuture<Iterable<T>>)query.iter(limit);
	}

	@SuppressWarnings("unchecked")
	public <T> SienaFuture<Iterable<T>> iter(int limit, Object offset) {
		return (SienaFuture<Iterable<T>>)query.iter(limit, offset);
	}

	@SuppressWarnings("unchecked")
	public <T> SienaFuture<Iterable<T>> iterPerPage(int limit) {
		return (SienaFuture<Iterable<T>>)query.iterPerPage(limit);
	}

	public <T> QueryAsync limit(int limit) {
		query.limit(limit);
		return this;
	}

	public <T> QueryAsync offset(Object offset) {
		query.offset(offset);
		return this;
	}

	public <T> QueryAsync paginate(int size) {
		query.paginate(size);
		return this;
	}

	public <T> QueryAsync nextPage() {
		query.nextPage();
		return this;
	}

	public <T> QueryAsync previousPage() {
		query.previousPage();
		return this;
	}

	public <T> QueryAsync customize(QueryOption... options) {
		query.customize(options);
		return this;
	}

	public <T> QueryAsync stateful() {
		query.stateful();
		return this;
	}

	public <T> QueryAsync stateless() {
		query.stateless();
		return this;
	}

	public <T> QueryAsync release() {
		query.release();
		return this;
	}

	public <T> QueryAsync resetData() {
		query.resetData();
		return this;
	}

	public <T> QueryAsync dump() {
		query.dump();
		return this;
	}

	public <T> QueryAsync restore(String dump) {
		query.restore(dump);
		return this;
	}

	public <T> Query sync() {
		return new Query(query.sync());
	}

	public PersistenceManagerAsync getPersistenceManager() {
		return query.getPersistenceManager();
	}


}

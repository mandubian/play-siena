package play.modules.siena;

import java.util.List;

import siena.core.batch.Batch;

public class BatchWrapper{
	@SuppressWarnings("rawtypes")
	Batch batch;
	
	@SuppressWarnings("rawtypes") 
	public <T> BatchWrapper(Batch batch){
		this.batch = batch;
	}
	
	public <T> BatchAsyncWrapper async() {
		return new BatchAsyncWrapper(batch.async());
	}

	@SuppressWarnings("unchecked")
	public <T> int delete(T... arg0) {
		return batch.delete(arg0);
	}

	@SuppressWarnings("unchecked")
	public <T> int delete(Iterable<T> arg0) {
		return batch.delete(arg0);
	}

	public <T> int deleteByKeys(T... arg0) {
		return batch.deleteByKeys(arg0);
	}

	@SuppressWarnings("unchecked")
	public <T> int deleteByKeys(Iterable<T> arg0) {
		return batch.deleteByKeys(arg0);
	}

	@SuppressWarnings("unchecked")
	public <T> int get(T... arg0) {
		return batch.get(arg0);
	}

	@SuppressWarnings("unchecked")
	public <T> int get(Iterable<T> arg0) {
		return batch.get(arg0);
	}

	@SuppressWarnings("unchecked")
	public <T> List<T> getByKeys(T... arg0) {
		return batch.getByKeys(arg0);
	}

	@SuppressWarnings("unchecked")
	public <T> List<T> getByKeys(Iterable<T> arg0) {
		return batch.getByKeys(arg0);
	}

	@SuppressWarnings("unchecked")
	public <T> int insert(T... arg0) {
		return batch.insert(arg0);
	}

	@SuppressWarnings("unchecked")
	public <T> int insert(Iterable<T> arg0) {
		return batch.insert(arg0);
	}

	@SuppressWarnings("unchecked")
	public <T> int update(T... arg0) {
		return batch.update(arg0);
	}

	@SuppressWarnings("unchecked")
	public <T> int update(Iterable<T> arg0) {
		return batch.update(arg0);
	}

}

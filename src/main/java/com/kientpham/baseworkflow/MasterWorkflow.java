package com.kientpham.baseworkflow;

import java.util.ArrayList;
import java.util.List;

/**
 * @author trungkienbk@gmail.com
 *
 * @param <T>
 *            Generic Transaction Model.
 * @param <D>
 *            Generic OmibusDTO.
 */
public class MasterWorkflow<T, D> {

	private List<BaseBuilder<T, D>> builderList;

	private BaseTransactionManager<T, D> baseTransactionManager;

	private List<BaseBuilder<T, D>> preExecuteBuilderList;

	private List<BaseBuilder<T, D>> postExecuteBuilderList;

	public void setPreExecuteBuilder(BaseBuilder<T, D> builder) {
		if (preExecuteBuilderList == null)
			preExecuteBuilderList = new ArrayList<BaseBuilder<T, D>>();

		preExecuteBuilderList.add(builder);
	}

	public void setPostExecuteBuilder(BaseBuilder<T, D> builder) {
		if (postExecuteBuilderList == null)
			postExecuteBuilderList = new ArrayList<BaseBuilder<T, D>>();

		postExecuteBuilderList.add(builder);
	}

	/**
	 * 
	 * @param baseTransaction
	 */
	public void setBaseTransactionManager(BaseTransactionManager<T, D> baseTransaction) {
		this.baseTransactionManager = baseTransaction;
	}

	/**
	 * @param builder
	 */
	public void setFirstBuilder(BaseBuilder<T, D> builder) {
		builderList = new ArrayList<BaseBuilder<T, D>>();
		builderList.add(builder);
	}

	/**
	 * @param builder
	 */
	public void setNextBuilder(BaseBuilder<T, D> builder) {
		builderList.add(builder);
	}

	/**
	 * Go through each transaction to process
	 * 
	 * @param transactionList
	 * @throws ServiceException
	 */
	public List<T> executeWorkflow(List<T> transactionList, BaseOmnibusDTO<T, D> baseOmniBusDTO)
			throws WorkflowException {
		if (baseTransactionManager == null || builderList == null) {
			throw new WorkflowException("Missing Transacion Manager or Builder List.");
		}
		if (transactionList == null || transactionList.size() == 0) {
			throw new WorkflowException("There is no transaction to process");
		}
		preExecute(baseOmniBusDTO);
		if (transactionList.size() == 1) {
			this.execute(transactionList.get(0), baseOmniBusDTO);
		} else {
			execute(transactionList, baseOmniBusDTO);
		}
		postExecute(baseOmniBusDTO);
		return transactionList;
	}

	private void preExecute(BaseOmnibusDTO<T, D> omniBusDTO) throws WorkflowException {
		if (preExecuteBuilderList != null) {
			for (BaseBuilder<T, D> builder : preExecuteBuilderList) {
				builder.execute(omniBusDTO);
			}
		}
	}

	private void execute(List<T> transactionList, BaseOmnibusDTO<T, D> omniBusDTO) {
		for (T transaction : transactionList) {
			this.execute(transaction, omniBusDTO);
		}
	}

	private void execute(T transaction, BaseOmnibusDTO<T, D> omniBusDTO) {
		try {
			omniBusDTO.setTransaction(transaction);
			for (BaseBuilder<T, D> builder : builderList) {
				builder.execute(omniBusDTO);
			}
		} catch (WorkflowException e) {
			baseTransactionManager.updateTransactionWhenException(transaction, e);
		} finally {
			baseTransactionManager.saveTransaction(transaction);
		}
	}

	private void postExecute(BaseOmnibusDTO<T, D> omniBusDTO) throws WorkflowException {
		if (postExecuteBuilderList != null) {
			for (BaseBuilder<T, D> builder : postExecuteBuilderList) {
				builder.execute(omniBusDTO);
			}
		}
	}
}

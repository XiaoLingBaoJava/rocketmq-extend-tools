package com.xiaolingbao.duplicate.persistence;
/**
 * @author: xiaolingbao
 * @date: 2022/5/22 10:30
 * @description: 
 */
public interface Persist {
    public String CONSUME_STATUS_CONSUMING = "CONSUMING";
    public String CONSUME_STATUS_CONSUMED = "CONSUMED";

    boolean doPersistence(PersistenceElement persistenceElement, long duplicateProcessingExpireMilliSeconds);

    void delete(PersistenceElement persistenceElement);

    void markConsumed(PersistenceElement persistenceElement, long duplicateRecordReserveMinutes);

    String get(PersistenceElement persistenceElement);

    default String persistenceElementToString(PersistenceElement persistenceElement) {
        return persistenceElement.toString();
    }

}
